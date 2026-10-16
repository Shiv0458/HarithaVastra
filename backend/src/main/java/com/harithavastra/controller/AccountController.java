package com.harithavastra.controller;

import com.harithavastra.dto.Requests.*;
import com.harithavastra.model.*;
import com.harithavastra.repository.*;
import com.harithavastra.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

  private final AuthService auth;
  private final OwnerService owners;
  private final AddressRepository addresses;
  private final ReviewRepository reviews;
  private final ShopOrderRepository orders;
  private final OrderItemRepository items;

  public AccountController(
    AuthService a,
    OwnerService o,
    AddressRepository ad,
    ReviewRepository r,
    ShopOrderRepository so,
    OrderItemRepository oi
  ) {
    auth = a;
    owners = o;
    addresses = ad;
    reviews = r;
    orders = so;
    items = oi;
  }

  @GetMapping
  Object me(Authentication a) {
    return auth.view(auth.user(owners.userId(a)));
  }

  @PutMapping
  Object update(@Valid @RequestBody Profile p, Authentication a) {
    return auth.update(owners.userId(a), p);
  }

  @GetMapping("/addresses")
  Object addresses(Authentication a) {
    return addresses.findByUserId(owners.userId(a));
  }

  @PostMapping("/addresses")
  Object add(@Valid @RequestBody Delivery d, Authentication a) {
    if (addresses.findByUserId(owners.userId(a)).size() >= 20) throw Errors.bad(
      "Maximum 20 saved addresses"
    );
    Address x = new Address();
    x.userId = owners.userId(a);
    x.name = d.name();
    x.phone = d.phone();
    x.line1 = d.line1();
    x.line2 = d.line2();
    x.city = d.city();
    x.state = d.state();
    x.pin = d.pin();
    return addresses.save(x);
  }

  @DeleteMapping("/addresses/{id}")
  Object remove(@PathVariable Long id, Authentication a) {
    Address x = addresses
      .findById(id)
      .filter(v -> v.userId.equals(owners.userId(a)))
      .orElseThrow(Errors::missing);
    addresses.delete(x);
    return Map.of("ok", true);
  }

  @PostMapping("/reviews/{productId}")
  Object review(
    @PathVariable Long productId,
    @Valid @RequestBody ReviewInput input,
    Authentication a
  ) {
    Long user = owners.userId(a);
    var paid = orders
      .findByOwnerKeyOrderByCreatedAtDesc("u:" + user)
      .stream()
      .filter(o -> "PAID".equals(o.status) && "DELIVERED".equals(o.fulfillment))
      .map(o -> o.id)
      .toList();
    if (
      paid.isEmpty() || !items.existsByProductIdAndOrderIdIn(productId, paid)
    ) throw Errors.bad(
      "Reviews are available after delivery of a purchased saree"
    );
    if (
      reviews.existsByProductIdAndUserId(productId, user)
    ) throw Errors.conflict("You have already reviewed this saree");
    Review r = new Review();
    r.productId = productId;
    r.userId = user;
    r.rating = input.rating();
    r.text = input.text();
    r.displayName = auth.user(user).name;
    r.createdAt = java.time.Instant.now();
    reviews.save(r);
    return Map.of("ok", true);
  }
}
