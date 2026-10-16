package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "product_images")
public class ProductImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long productId;
  public String url;
  public String label;
  public int position;
}
