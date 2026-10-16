package com.harithavastra.controller;

import com.harithavastra.dto.Requests.*;
import com.harithavastra.repository.*;
import com.harithavastra.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CheckoutController {

  private final CheckoutService checkout;
  private final PaymentService payments;
  private final PaymentGateway gateway;
  private final OwnerService owners;
  private final CartService carts;
  private final ShopOrderRepository orders;

  public CheckoutController(
    CheckoutService c,
    PaymentService p,
    PaymentGateway g,
    OwnerService o,
    CartService cs,
    ShopOrderRepository or
  ) {
    checkout = c;
    payments = p;
    gateway = g;
    owners = o;
    carts = cs;
    orders = or;
  }

  @PostMapping("/checkout")
  Object checkout(
    @Valid @RequestBody Checkout r,
    @RequestHeader("Idempotency-Key") String key,
    Authentication a,
    HttpServletRequest req
  ) {
    if (!gateway.configured()) throw Errors.conflict(
      "Razorpay credentials are not configured. Shopping works; payments are unavailable."
    );
    String owner = owners.owner(a, req);
    carts.ensure(owner);
    if (
      "live".equals(gateway.mode()) &&
      carts
        .cart(owner)
        .stream()
        .anyMatch(row ->
          Boolean.TRUE.equals(
            ((java.util.Map<?, ?>) row.get("product")).get("demo")
          )
        )
    ) throw Errors.conflict(
      "Demonstration products cannot be purchased in live mode"
    );
    var order = checkout.reserve(owner, key, r.delivery());
    return checkout.view(payments.initialize(order.id));
  }

  @GetMapping("/orders")
  Object list(Authentication a, HttpServletRequest r) {
    return orders
      .findByOwnerKeyOrderByCreatedAtDesc(owners.owner(a, r))
      .stream()
      .map(checkout::view)
      .toList();
  }

  @GetMapping("/orders/{id}")
  Object order(
    @PathVariable String id,
    Authentication a,
    HttpServletRequest r
  ) {
    return checkout.view(checkout.owned(id, owners.owner(a, r)));
  }

  @PostMapping("/orders/{id}/verify")
  Object verify(
    @PathVariable String id,
    @Valid @RequestBody Verify v,
    Authentication a,
    HttpServletRequest r
  ) {
    checkout.owned(id, owners.owner(a, r));
    return checkout.view(payments.verify(id, v));
  }

  @PostMapping("/orders/{id}/refresh")
  Object refresh(
    @PathVariable String id,
    Authentication a,
    HttpServletRequest r
  ) {
    checkout.owned(id, owners.owner(a, r));
    return checkout.view(payments.reconcile(id, true));
  }

  @PostMapping("/orders/{id}/cancel")
  Object cancel(
    @PathVariable String id,
    Authentication a,
    HttpServletRequest r
  ) {
    checkout.owned(id, owners.owner(a, r));
    return checkout.view(payments.cancel(id));
  }

  @PostMapping("/payments/webhook")
  Object webhook(
    @RequestBody byte[] raw,
    @RequestHeader("X-Razorpay-Signature") String signature,
    @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String id
  ) {
    payments.webhook(raw, signature, id);
    return Map.of("ok", true);
  }

  @PostMapping("/orders/{id}/sync-cart")
  Object syncCart(
    @PathVariable String id,
    Authentication a,
    HttpServletRequest r
  ) {
    String owner = owners.owner(a, r);
    checkout.owned(id, owner);
    carts.consume(owner, id);
    return Map.of("ok", true);
  }
}
