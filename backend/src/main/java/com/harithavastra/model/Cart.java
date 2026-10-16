package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "carts")
public class Cart {

  @Id
  public String ownerKey;

  @Version
  public long version;
}
