package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reviews")
public class Review {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long productId;
  public Long userId;
  public int rating;
  public String text;
  public String displayName;
  public java.time.Instant createdAt;
}
