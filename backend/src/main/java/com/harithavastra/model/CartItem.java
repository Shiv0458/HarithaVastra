package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cart_items")
public class CartItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String ownerKey;
  public Long productId;
  public int quantity;
}
