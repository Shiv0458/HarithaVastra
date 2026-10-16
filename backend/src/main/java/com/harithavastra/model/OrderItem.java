package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String orderId;
  public Long productId;
  public String name;
  public String collection;
  public String image;

  @Column(precision = 12, scale = 2)
  public java.math.BigDecimal unitPrice;

  @Column(precision = 12, scale = 2)
  public java.math.BigDecimal originalPrice;

  public int quantity;
}
