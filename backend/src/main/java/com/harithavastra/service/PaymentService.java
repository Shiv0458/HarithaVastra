package com.harithavastra.service;

import com.fasterxml.jackson.databind.*;
import com.harithavastra.dto.Requests.*;
import com.harithavastra.model.*;
import com.harithavastra.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

  private final ShopOrderRepository orders;
  private final OrderItemRepository items;
  private final ProductRepository products;
  private final PaymentRepository payments;
  private final WebhookEventRepository events;
  private final PaymentGateway gateway;
  private final ObjectMapper json;

  public PaymentService(
    ShopOrderRepository o,
    OrderItemRepository i,
    ProductRepository p,
    PaymentRepository pay,
    WebhookEventRepository e,
    PaymentGateway g,
    ObjectMapper j
  ) {
    orders = o;
    items = i;
    products = p;
    payments = pay;
    events = e;
    gateway = g;
    json = j;
  }

  @Transactional
  public ShopOrder initialize(String id) {
    ShopOrder o = orders.lock(id).orElseThrow(Errors::missing);
    if (!"CREATED".equals(o.status)) return o;
    // A gateway timeout may be ambiguous: never automatically create a second gateway order.
    try {
      o.gatewayOrderId = gateway.createOrder(o.id, Totals.paise(o.total));
      o.status = "PAYMENT_PENDING";
    } catch (org.springframework.web.server.ResponseStatusException e) {
      o.status = "GATEWAY_ERROR";
    }
    return o;
  }

  @Transactional
  public ShopOrder verify(String id, Verify r) {
    ShopOrder o = orders.lock(id).orElseThrow(Errors::missing);
    if (
      !Objects.equals(o.gatewayOrderId, r.razorpay_order_id()) ||
      !gateway.verify(
        o.gatewayOrderId,
        r.razorpay_payment_id(),
        r.razorpay_signature()
      )
    ) throw Errors.bad("Invalid payment signature");
    apply(o, gateway.payment(r.razorpay_payment_id()));
    return o;
  }

  @Transactional
  public ShopOrder reconcile(String id, boolean expire) {
    ShopOrder o = orders.lock(id).orElseThrow(Errors::missing);
    if (
      o.gatewayOrderId != null &&
      !List.of("PAID", "PAID_REVIEW").contains(o.status)
    ) {
      for (JsonNode payment : gateway.payments(o.gatewayOrderId))
        apply(o, payment);
    }
    if (
      expire &&
      !List.of("PAID", "PAID_REVIEW").contains(o.status) &&
      o.expiresAt.isBefore(Instant.now())
    ) release(o);
    return o;
  }

  @Transactional
  public ShopOrder cancel(String id) {
    ShopOrder o = orders.lock(id).orElseThrow(Errors::missing);
    if (o.gatewayOrderId != null) for (JsonNode p : gateway.payments(
      o.gatewayOrderId
    ))
      apply(o, p);
    if (
      List.of("PAID", "PAID_REVIEW").contains(o.status)
    ) throw Errors.conflict(
      "Payment already captured. Contact support for a refund."
    );
    release(o);
    o.status = "CANCELLED";
    return o;
  }

  private void release(ShopOrder o) {
    if (!o.reservationReleased) {
      for (OrderItem i : items.findByOrderIdOrderByProductId(o.id)) {
        Product p = products.lockById(i.productId).orElseThrow();
        p.stock += i.quantity;
      }
      o.reservationReleased = true;
    }
    o.status = "EXPIRED";
  }

  private void apply(ShopOrder o, JsonNode p) {
    if (
      !Objects.equals(o.gatewayOrderId, p.path("order_id").asText()) ||
      Totals.paise(o.total) != p.path("amount").asLong(-1) ||
      !"INR".equals(p.path("currency").asText())
    ) throw Errors.bad("Payment order, amount or currency mismatch");
    String id = p.path("id").asText();
    if (id.isBlank()) throw Errors.bad("Missing payment id");
    Payment payment = payments.findById(id).orElseGet(Payment::new);
    if (
      payment.orderId != null && !payment.orderId.equals(o.id)
    ) throw Errors.bad("Payment belongs to another order");
    payment.id = id;
    payment.orderId = o.id;
    payment.amount = p.path("amount").asLong();
    payment.currency = "INR";
    payment.status = p.path("status").asText();
    payment.updatedAt = Instant.now();
    payments.save(payment);
    if (!"captured".equals(payment.status)) {
      return;
    }
    if (o.paymentId != null) {
      if (!o.paymentId.equals(id)) o.status = "PAID_REVIEW";
      return;
    }
    // Captures can arrive after cancellation/expiry. Reacquire all stock atomically or flag a refund review.
    if (o.reservationReleased) {
      List<OrderItem> lines = items.findByOrderIdOrderByProductId(o.id);
      List<Product> locked = new ArrayList<>();
      boolean available = true;
      for (OrderItem i : lines) {
        Product prod = products.lockById(i.productId).orElseThrow();
        locked.add(prod);
        if (prod.archived || prod.stock < i.quantity) available = false;
      }
      if (!available) {
        o.paymentId = id;
        o.status = "PAID_REVIEW";
        return;
      }
      for (int n = 0; n < lines.size(); n++) locked.get(n).stock -= lines
        .get(n)
        .quantity;
      o.reservationReleased = false;
    }
    o.paymentId = id;
    o.status = "PAID";
  }

  @Transactional
  public void webhook(byte[] raw, String signature, String eventId) {
    if (!gateway.verifyWebhook(raw, signature)) throw Errors.bad(
      "Invalid webhook signature"
    );
    String key =
      eventId != null && eventId.length() <= 100
        ? eventId
        : AuthService.hash(
            new String(raw, java.nio.charset.StandardCharsets.UTF_8)
          );
    if (events.existsById(key)) return;
    try {
      JsonNode body = json.readTree(raw);
      String type = body.path("event").asText();
      if (
        List.of(
          "payment.captured",
          "payment.failed",
          "payment.authorized"
        ).contains(type)
      ) {
        JsonNode entity = body.path("payload").path("payment").path("entity");
        String gatewayOrderId = entity.path("order_id").asText();
        var found = orders.findByGatewayOrderId(gatewayOrderId);
        if (found.isEmpty()) return;
        ShopOrder order = orders.lock(found.get().id).orElseThrow();
        if (events.existsById(key)) return;
        // Fetch current gateway state so delayed events cannot regress captured status.
        apply(order, gateway.payment(entity.path("id").asText()));
      }
      WebhookEvent e = new WebhookEvent();
      e.id = key;
      e.processedAt = Instant.now();
      events.save(e);
    } catch (java.io.IOException e) {
      throw Errors.bad("Invalid webhook payload");
    }
  }
}
