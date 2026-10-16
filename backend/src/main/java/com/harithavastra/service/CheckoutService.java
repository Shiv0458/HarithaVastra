package com.harithavastra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harithavastra.dto.Requests.*;
import com.harithavastra.model.*;
import com.harithavastra.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

  private final CartService carts;
  private final CartItemRepository cartItems;
  private final ProductRepository products;
  private final ProductImageRepository images;
  private final ShopOrderRepository orders;
  private final OrderItemRepository orderItems;
  private final ObjectMapper json;
  private final BigDecimal shipping, taxRate;

  public CheckoutService(
    CartService c,
    CartItemRepository ci,
    ProductRepository p,
    ProductImageRepository im,
    ShopOrderRepository o,
    OrderItemRepository oi,
    ObjectMapper j,
    @Value("${app.shipping}") BigDecimal s,
    @Value("${app.tax-rate}") BigDecimal t
  ) {
    carts = c;
    cartItems = ci;
    products = p;
    images = im;
    orders = o;
    orderItems = oi;
    json = j;
    shipping = s;
    taxRate = t;
    if (
      s.signum() < 0 || t.signum() < 0 || t.compareTo(BigDecimal.ONE) > 0
    ) throw new IllegalArgumentException("Invalid shipping/tax settings");
  }

  @Transactional
  public Totals.Result quote(String owner) {
    carts.lock(owner);
    return totals(cartItems.findByOwnerKeyOrderByProductId(owner), false);
  }

  private Totals.Result totals(List<CartItem> items, boolean lock) {
    List<Totals.Line> lines = new ArrayList<>();
    for (CartItem item : items) {
      Product p = (
        lock
          ? products.lockById(item.productId)
          : products.findById(item.productId)
      ).orElseThrow(Errors::missing);
      if (p.archived || p.stock < item.quantity) throw Errors.conflict(
        p.name + " is unavailable in this quantity. Update your bag."
      );
      lines.add(new Totals.Line(p.price, p.originalPrice, item.quantity));
    }
    return Totals.calculate(lines, shipping, taxRate);
  }

  @Transactional
  public ShopOrder reserve(String owner, String key, Delivery delivery) {
    if (key == null || !key.matches("[a-zA-Z0-9-]{16,80}")) throw Errors.bad(
      "A valid Idempotency-Key is required"
    );
    carts.lock(owner);
    String payload;
    try {
      payload = json.writeValueAsString(delivery);
    } catch (Exception e) {
      throw Errors.bad("Invalid delivery details");
    }
    var old = orders.findByOwnerKeyAndIdempotencyKey(owner, key);
    if (old.isPresent()) {
      if (
        !old.get().requestHash.equals(AuthService.hash(payload))
      ) throw Errors.conflict(
        "Checkout key reused with different delivery details"
      );
      return old.get();
    }
    List<CartItem> lines = cartItems.findByOwnerKeyOrderByProductId(owner);
    Totals.Result t = totals(lines, true);
    ShopOrder o = new ShopOrder();
    o.id =
      "HV-" +
      UUID.randomUUID()
        .toString()
        .replace("-", "")
        .substring(0, 24)
        .toUpperCase();
    o.ownerKey = owner;
    o.userId = owner.startsWith("u:")
      ? Long.parseLong(owner.substring(2))
      : null;
    o.idempotencyKey = key;
    o.requestHash = AuthService.hash(payload);
    o.delivery = payload;
    o.status = "CREATED";
    o.fulfillment = "UNFULFILLED";
    o.subtotal = t.subtotal();
    o.discount = t.discount();
    o.shipping = t.shipping();
    o.tax = t.tax();
    o.total = t.total();
    o.createdAt = Instant.now();
    o.expiresAt = o.createdAt.plus(Duration.ofMinutes(30));
    orders.saveAndFlush(o);
    for (CartItem line : lines) {
      Product p = products.findById(line.productId).orElseThrow();
      p.stock -= line.quantity;
      OrderItem i = new OrderItem();
      i.orderId = o.id;
      i.productId = p.id;
      i.name = p.name;
      i.collection = p.collection;
      i.unitPrice = p.price;
      i.originalPrice = p.originalPrice;
      i.quantity = line.quantity;
      i.image = images
        .findByProductIdOrderByPositionAsc(p.id)
        .stream()
        .findFirst()
        .map(im -> im.url)
        .orElse("");
      orderItems.save(i);
    }
    return o;
  }

  public ShopOrder owned(String id, String owner) {
    return orders
      .findById(id)
      .filter(o -> o.ownerKey.equals(owner))
      .orElseThrow(Errors::missing);
  }

  public Map<String, Object> view(ShopOrder o) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", o.id);
    m.put("status", o.status);
    m.put("fulfillment", o.fulfillment);
    m.put("gatewayOrderId", o.gatewayOrderId);
    m.put("subtotal", o.subtotal);
    m.put("discount", o.discount);
    m.put("shipping", o.shipping);
    m.put("tax", o.tax);
    m.put("total", o.total);
    m.put("createdAt", o.createdAt);
    m.put("expiresAt", o.expiresAt);
    m.put("items", orderItems.findByOrderIdOrderByProductId(o.id));
    try {
      m.put("delivery", json.readValue(o.delivery, Map.class));
    } catch (Exception ignored) {}
    return m;
  }
}
