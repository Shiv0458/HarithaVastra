package com.harithavastra.controller;

import com.harithavastra.dto.Requests.*;
import com.harithavastra.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ShoppingController {

  private final CartService carts;
  private final OwnerService owners;
  private final CheckoutService checkout;

  public ShoppingController(CartService c, OwnerService o, CheckoutService ch) {
    carts = c;
    owners = o;
    checkout = ch;
  }

  private String owner(Authentication a, HttpServletRequest r) {
    String key = owners.owner(a, r);
    carts.ensure(key);
    return key;
  }

  @GetMapping("/cart")
  Object cart(Authentication a, HttpServletRequest r) {
    return carts.cart(owner(a, r));
  }

  @PutMapping("/cart/items/{id}")
  Object quantity(
    @PathVariable Long id,
    @Valid @RequestBody Quantity q,
    Authentication a,
    HttpServletRequest r
  ) {
    String key = owner(a, r);
    carts.quantity(key, id, q.quantity());
    return carts.cart(key);
  }

  @GetMapping("/cart/quote")
  Object quote(Authentication a, HttpServletRequest r) {
    return checkout.quote(owner(a, r));
  }

  @GetMapping("/wishlist")
  Object wishes(Authentication a, HttpServletRequest r) {
    return carts.wishlist(owner(a, r));
  }

  @PutMapping("/wishlist/{id}")
  Object wish(@PathVariable Long id, Authentication a, HttpServletRequest r) {
    String key = owner(a, r);
    carts.wish(key, id, true);
    return carts.wishlist(key);
  }

  @DeleteMapping("/wishlist/{id}")
  Object unwish(@PathVariable Long id, Authentication a, HttpServletRequest r) {
    String key = owner(a, r);
    carts.wish(key, id, false);
    return carts.wishlist(key);
  }

  @PostMapping("/wishlist/{id}/move")
  Object move(@PathVariable Long id, Authentication a, HttpServletRequest r) {
    String key = owner(a, r);
    carts.move(key, id);
    return Map.of("cart", carts.cart(key), "wishlist", carts.wishlist(key));
  }
}
