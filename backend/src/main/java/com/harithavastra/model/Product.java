package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String name;
  public Long categoryId;
  public String collection;
  public String occasion;
  public String colour;
  public String description;

  @Column(precision = 12, scale = 2)
  public java.math.BigDecimal price;

  @Column(precision = 12, scale = 2)
  public java.math.BigDecimal originalPrice;

  public int stock;
  public boolean archived;
  public boolean demo;
  public java.time.Instant createdAt;

  @Column(columnDefinition = "TEXT")
  public String specifications;

  @Version
  public long version;
}
