package com.harithavastra;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harithavastra.dto.Requests.*;
import com.harithavastra.model.*;
import com.harithavastra.repository.*;
import com.harithavastra.service.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommerceIntegrationTest {

  @Autowired
  CheckoutService checkout;

  @Autowired
  CartService carts;

  @Autowired
  CatalogService catalog;

  @Autowired
  PaymentService paymentService;

  @Autowired
  ProductRepository products;

  @Autowired
  CartItemRepository cartItems;

  @Autowired
  ShopOrderRepository orders;

  @Autowired
  PaymentRepository payments;

  @Autowired
  OrderItemRepository orderItems;

  @Autowired
  AuthService auth;

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper json;

  @MockitoBean
  PaymentGateway gateway;

  final Delivery delivery = new Delivery(
    "Test Customer",
    "9876543210",
    "test@example.com",
    "12 Demo Road",
    "",
    "Bengaluru",
    "Karnataka",
    "560001"
  );

  @BeforeEach
  void setup() {
    when(gateway.configured()).thenReturn(true);
    when(gateway.mode()).thenReturn("test");
    when(gateway.keyId()).thenReturn("rzp_test_fixture");
    when(gateway.createOrder(anyString(), anyLong())).thenAnswer(
      a -> "order_" + a.getArgument(0).toString().replace("-", "")
    );
  }

  Product product(int stock) {
    Product p = new Product();
    p.name = "Fixture saree";
    p.collection = "Test";
    p.description = "Test-only fixture";
    p.price = new BigDecimal("1000.00");
    p.originalPrice = new BigDecimal("1200.00");
    p.stock = stock;
    p.createdAt = Instant.now();
    p.specifications = "{}";
    return products.saveAndFlush(p);
  }

  String owner(Product p, int qty) {
    String owner = "g:" + UUID.randomUUID();
    carts.ensure(owner);
    carts.quantity(owner, p.id, qty);
    return owner;
  }

  String key() {
    return UUID.randomUUID().toString();
  }

  @Test
  void serverTotalsSnapshotAndIdempotentReservation() {
    Product p = product(3);
    String owner = owner(p, 2),
      key = key();
    ShopOrder a = checkout.reserve(owner, key, delivery);
    ShopOrder b = checkout.reserve(owner, key, delivery);
    assertThat(a.id).isEqualTo(b.id);
    assertThat(a.total).isEqualByComparingTo("2200.00");
    assertThat(products.findById(p.id).orElseThrow().stock).isEqualTo(1);
    assertThat(orderItems.findByOrderIdOrderByProductId(a.id)).hasSize(1);
    p = products.findById(p.id).orElseThrow();
    p.price = new BigDecimal("9000");
    p.originalPrice = null;
    products.saveAndFlush(p);
    assertThat(
      orderItems.findByOrderIdOrderByProductId(a.id).get(0).unitPrice
    ).isEqualByComparingTo("1000");
  }

  @Test
  void concurrentCustomersCannotOversell() throws Exception {
    Product p = product(1);
    String a = owner(p, 1),
      b = owner(p, 1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch latch = new CountDownLatch(1);
    try {
      List<Future<Boolean>> results = new ArrayList<>();
      for (String owner : List.of(a, b))
        results.add(
          pool.submit(() -> {
            latch.await();
            try {
              checkout.reserve(owner, key(), delivery);
              return true;
            } catch (
              org.springframework.web.server.ResponseStatusException ex
            ) {
              return false;
            }
          })
        );
      latch.countDown();
      int successes = 0;
      for (var f : results) if (f.get(15, TimeUnit.SECONDS)) successes++;
      assertThat(successes).isEqualTo(1);
      assertThat(products.findById(p.id).orElseThrow().stock).isZero();
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void duplicateCaptureDoesNotDeductTwiceAndTamperingFails() throws Exception {
    Product p = product(2);
    String owner = owner(p, 1);
    ShopOrder o = paymentService.initialize(
      checkout.reserve(owner, key(), delivery).id
    );
    String paymentId = "pay_" + UUID.randomUUID().toString().replace("-", "");
    var entity = json.readTree(
      "{\"id\":\"" +
        paymentId +
        "\",\"order_id\":\"" +
        o.gatewayOrderId +
        "\",\"status\":\"captured\",\"amount\":115000,\"currency\":\"INR\"}"
    );
    when(gateway.verify(o.gatewayOrderId, paymentId, "valid")).thenReturn(true);
    when(gateway.payment(paymentId)).thenReturn(entity);
    assertThatThrownBy(() ->
      paymentService.verify(
        o.id,
        new Verify(o.gatewayOrderId, paymentId, "invalid")
      )
    ).isInstanceOf(
      org.springframework.web.server.ResponseStatusException.class
    );
    paymentService.verify(
      o.id,
      new Verify(o.gatewayOrderId, paymentId, "valid")
    );
    paymentService.verify(
      o.id,
      new Verify(o.gatewayOrderId, paymentId, "valid")
    );
    assertThat(products.findById(p.id).orElseThrow().stock).isEqualTo(1);
    assertThat(orders.findById(o.id).orElseThrow().status).isEqualTo("PAID");
    assertThat(payments.findByOrderId(o.id)).hasSize(1);
  }

  @Test
  void amountMismatchCannotMarkPaid() throws Exception {
    Product p = product(1);
    ShopOrder o = paymentService.initialize(
      checkout.reserve(owner(p, 1), key(), delivery).id
    );
    when(gateway.verify(any(), any(), any())).thenReturn(true);
    when(gateway.payment("pay_bad")).thenReturn(
      json.readTree(
        "{\"id\":\"pay_bad\",\"order_id\":\"" +
          o.gatewayOrderId +
          "\",\"status\":\"captured\",\"amount\":1,\"currency\":\"INR\"}"
      )
    );
    assertThatThrownBy(() ->
      paymentService.verify(
        o.id,
        new Verify(o.gatewayOrderId, "pay_bad", "valid")
      )
    ).isInstanceOf(
      org.springframework.web.server.ResponseStatusException.class
    );
    assertThat(orders.findById(o.id).orElseThrow().status).isEqualTo(
      "PAYMENT_PENDING"
    );
  }

  @Test
  void cancellationIsIdempotentAndLateCaptureNeedsReviewWhenStockGone()
    throws Exception {
    Product p = product(1);
    ShopOrder o = paymentService.initialize(
      checkout.reserve(owner(p, 1), key(), delivery).id
    );
    when(gateway.payments(o.gatewayOrderId)).thenReturn(json.readTree("[]"));
    paymentService.cancel(o.id);
    paymentService.cancel(o.id);
    assertThat(products.findById(p.id).orElseThrow().stock).isEqualTo(1);
    checkout.reserve(owner(p, 1), key(), delivery);
    String paymentId = "pay_late";
    when(gateway.verify(any(), any(), any())).thenReturn(true);
    when(gateway.payment(paymentId)).thenReturn(
      json.readTree(
        "{\"id\":\"pay_late\",\"order_id\":\"" +
          o.gatewayOrderId +
          "\",\"status\":\"captured\",\"amount\":115000,\"currency\":\"INR\"}"
      )
    );
    assertThat(
      paymentService
        .verify(o.id, new Verify(o.gatewayOrderId, paymentId, "valid"))
        .status
    ).isEqualTo("PAID_REVIEW");
    assertThat(products.findById(p.id).orElseThrow().stock).isZero();
  }

  @Test
  void guestMergeUnionsWishlistAndDoesNotDoubleQuantities() {
    Product p = product(5);
    String guest = owner(p, 2),
      user = owner(p, 1);
    carts.wish(guest, p.id, true);
    carts.merge(guest, user);
    carts.merge(guest, user);
    assertThat(
      cartItems.findByOwnerKeyAndProductId(user, p.id).orElseThrow().quantity
    ).isEqualTo(2);
    assertThat(carts.wishlist(user)).hasSize(1);
    assertThat(carts.cart(guest)).isEmpty();
  }

  @Test
  void authorizationAndOrderIsolation() throws Exception {
    mvc
      .perform(get("/api/admin/products"))
      .andExpect(status().isUnauthorized());
    var user = auth.register(
      new Register(
        "Customer",
        "c" + UUID.randomUUID() + "@example.com",
        "long-password"
      )
    );
    String token = (String) auth.issue(user).get("token");
    mvc
      .perform(
        get("/api/admin/products").header("Authorization", "Bearer " + token)
      )
      .andExpect(status().isForbidden());
    Product p = product(2);
    ShopOrder o = checkout.reserve(owner(p, 1), key(), delivery);
    mvc
      .perform(
        get("/api/orders/" + o.id).header(
          "X-Guest-Token",
          UUID.randomUUID().toString()
        )
      )
      .andExpect(status().isNotFound());
  }

  @Test
  void publicCataloguePaginationRatingAndFacets() throws Exception {
    Product p = product(2);
    mvc
      .perform(
        get("/api/products")
          .param("q", "Fixture")
          .param("sort", "rating")
          .param("size", "2")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items").isArray());
    mvc.perform(get("/api/products/facets")).andExpect(status().isOk());
  }

  @Test
  void archivedCartItemsCanBeRemoved() {
    Product p = product(2);
    String owner = owner(p, 1);
    catalog.archive(p.id);
    assertThat(carts.cart(owner)).hasSize(1);
    carts.quantity(owner, p.id, 0);
    assertThat(carts.cart(owner)).isEmpty();
  }

  @Test
  void signedWebhookDuplicateIsIdempotent() throws Exception {
    Product p = product(2);
    ShopOrder o = paymentService.initialize(
      checkout.reserve(owner(p, 1), key(), delivery).id
    );
    String paymentId =
      "pay_webhook" + UUID.randomUUID().toString().replace("-", "");
    var entity = json
      .createObjectNode()
      .put("id", paymentId)
      .put("order_id", o.gatewayOrderId)
      .put("amount", 115000)
      .put("currency", "INR")
      .put("status", "captured");
    when(gateway.payment(paymentId)).thenReturn(entity);
    when(gateway.verifyWebhook(any(), any())).thenReturn(true);
    var body = json.createObjectNode().put("event", "payment.captured");
    body.putObject("payload").putObject("payment").set("entity", entity);
    String event = UUID.randomUUID().toString();
    paymentService.webhook(json.writeValueAsBytes(body), "fixture", event);
    paymentService.webhook(json.writeValueAsBytes(body), "fixture", event);
    assertThat(payments.findByOrderId(o.id)).hasSize(1);
    assertThat(products.findById(p.id).orElseThrow().stock).isEqualTo(1);
    assertThat(orders.findById(o.id).orElseThrow().status).isEqualTo("PAID");
  }

  @Test
  void invalidWebhookCannotChangeOrder() throws Exception {
    when(gateway.verifyWebhook(any(), any())).thenReturn(false);
    mvc
      .perform(
        post("/api/payments/webhook")
          .header("X-Razorpay-Signature", "bad")
          .contentType("application/json")
          .content("{}")
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void paidCartCleanupIsIdempotentAndPreservesOtherItems() throws Exception {
    Product p = product(4),
      other = product(2);
    String owner = owner(p, 1);
    ShopOrder order = paymentService.initialize(
      checkout.reserve(owner, key(), delivery).id
    );
    carts.quantity(owner, other.id, 1);
    String paymentId =
      "pay_cleanup" + UUID.randomUUID().toString().replace("-", "");
    when(gateway.verify(any(), any(), any())).thenReturn(true);
    when(gateway.payment(paymentId)).thenReturn(
      json
        .createObjectNode()
        .put("id", paymentId)
        .put("order_id", order.gatewayOrderId)
        .put("amount", 115000)
        .put("currency", "INR")
        .put("status", "captured")
    );
    paymentService.verify(
      order.id,
      new Verify(order.gatewayOrderId, paymentId, "valid")
    );
    carts.consume(owner, order.id);
    carts.quantity(owner, p.id, 1);
    carts.consume(owner, order.id);
    assertThat(
      cartItems.findByOwnerKeyAndProductId(owner, p.id).orElseThrow().quantity
    ).isEqualTo(1);
    assertThat(
      cartItems
        .findByOwnerKeyAndProductId(owner, other.id)
        .orElseThrow()
        .quantity
    ).isEqualTo(1);
  }
}
