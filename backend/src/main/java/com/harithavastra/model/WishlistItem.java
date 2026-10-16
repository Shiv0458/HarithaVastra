package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "wishlists")
public class WishlistItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String ownerKey;
  public Long productId;
}
