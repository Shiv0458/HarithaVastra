package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "payments")
public class Payment {

  @Id
  public String id;

  public String orderId;
  public String status;
  public long amount;
  public String currency;
  public java.time.Instant updatedAt;
}
