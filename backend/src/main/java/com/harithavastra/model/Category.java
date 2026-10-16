package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(unique = true)
  public String name;

  public String slug;
}
