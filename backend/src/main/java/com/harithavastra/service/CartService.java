package com.harithavastra.service;

import com.harithavastra.model.*;
import com.harithavastra.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

  private final ShopOrderRepository orders;
  private final OrderItemRepository orderItems;
  private final CartRepository carts;
  private final CartItemRepository items;
  private final WishlistItemRepository wishes;
  private final CatalogService catalog;

  public CartService(
    ShopOrderRepository orders,
    OrderItemRepository orderItems,
    CartRepository c,
    CartItemRepository i,
    WishlistItemRepository w,
    CatalogService p
  ) {
    this.orders = orders;
    this.orderItems = orderItems;
    carts = c;
    items = i;
    wishes = w;
    catalog = p;
  }

  @Transactional
  public void ensure(String owner) {
    if (!carts.existsById(owner)) {
      Cart c = new Cart();
      c.ownerKey = owner;
      carts.saveAndFlush(c);
    }
  }

  public void lock(String owner) {
    carts
      .lock(owner)
      .orElseThrow(() -> Errors.bad("Initialize shopping session first"));
  }

  public List<Map<String, Object>> cart(String owner) {
    return items
      .findByOwnerKeyOrderByProductId(owner)
      .stream()
      .map(i -> {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(
          "product",
          catalog.view(catalog.getIncludingArchived(i.productId))
        );
        row.put("quantity", i.quantity);
        return row;
      })
      .toList();
  }

  @Transactional
  public void quantity(String owner, Long id, int quantity) {
    lock(owner);
    CartItem item = items
      .findByOwnerKeyAndProductId(owner, id)
      .orElseGet(CartItem::new);
    if (quantity == 0) {
      if (item.id != null) items.delete(item);
      return;
    }
    Product p = catalog.get(id);
    if (quantity > p.stock) throw Errors.conflict(
      "Only " + p.stock + " available for " + p.name
    );
    item.ownerKey = owner;
    item.productId = id;
    item.quantity = quantity;
    items.save(item);
  }

  public List<Map<String, Object>> wishlist(String owner) {
    return wishes
      .findByOwnerKey(owner)
      .stream()
      .map(i -> {
        try {
          return catalog.view(catalog.get(i.productId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
          return null;
        }
      })
      .filter(Objects::nonNull)
      .toList();
  }

  @Transactional
  public void wish(String owner, Long id, boolean enabled) {
    lock(owner);
    var old = wishes.findByOwnerKeyAndProductId(owner, id);
    if (!enabled) {
      old.ifPresent(wishes::delete);
      return;
    }
    catalog.get(id);
    if (old.isEmpty()) {
      WishlistItem i = new WishlistItem();
      i.ownerKey = owner;
      i.productId = id;
      wishes.save(i);
    }
  }

  @Transactional
  public void move(String owner, Long id) {
    lock(owner);
    int q = items
      .findByOwnerKeyAndProductId(owner, id)
      .map(i -> i.quantity)
      .orElse(0);
    if (q >= 10) throw Errors.bad("Maximum quantity is 10");
    quantity(owner, id, q + 1);
    wish(owner, id, false);
  }

  @Transactional
  public void merge(String guest, String user) {
    List.of(guest, user).stream().sorted().forEach(this::lock);
    for (CartItem i : items.findByOwnerKeyOrderByProductId(guest)) {
      try {
        Product p = catalog.get(i.productId);
        int existing = items
          .findByOwnerKeyAndProductId(user, i.productId)
          .map(v -> v.quantity)
          .orElse(0);
        quantity(
          user,
          i.productId,
          Math.min(p.stock, Math.min(10, Math.max(existing, i.quantity)))
        );
      } catch (
        org.springframework.web.server.ResponseStatusException ignored
      ) {}
    }
    for (WishlistItem i : wishes.findByOwnerKey(guest)) {
      try {
        wish(user, i.productId, true);
      } catch (
        org.springframework.web.server.ResponseStatusException ignored
      ) {}
    }
    items.deleteByOwnerKey(guest);
    wishes.deleteByOwnerKey(guest);
  }

  /** Consume the purchased quantities exactly once after verified capture. */
  @Transactional
  public void consume(String owner, String orderId) {
    lock(owner);
    ShopOrder order = orders
      .lock(orderId)
      .filter(o -> o.ownerKey.equals(owner))
      .orElseThrow(Errors::missing);
    if (!"PAID".equals(order.status) || order.cartCleared) return;
    for (OrderItem line : orderItems.findByOrderIdOrderByProductId(orderId)) {
      items
        .findByOwnerKeyAndProductId(owner, line.productId)
        .ifPresent(item -> {
          item.quantity = Math.max(0, item.quantity - line.quantity);
          if (item.quantity == 0) items.delete(item);
        });
    }
    order.cartCleared = true;
  }
}
