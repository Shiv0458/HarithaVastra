package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class ShopOrder {

  @Id
  public String id;

  public String ownerKey;
  public String idempotencyKey;
  public String requestHash;
  public Long userId;
  public String status;
  public String fulfillment;
  public String gatewayOrderId;
  public String paymentId;

  @Column(columnDefinition = "TEXT")
  public String delivery;

  @Column(precision = 12, scale = 2)
  public java.math.BigDecimal subtotal;

  @Column(precision = 12, scale = 2)
  public java.math.BigDecimal discount;

  @Column(precision = 12, scale = 2)
  public java.math.BigDecimal shipping;

  @Column(precision = 12, scale = 2)
  public java.math.BigDecimal tax;

  @Column(precision = 12, scale = 2)
  public java.math.BigDecimal total;

  public java.time.Instant createdAt;
  public java.time.Instant expiresAt;
  public boolean reservationReleased;
  public boolean cartCleared;

  @Version
  public long version;
}
