package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "addresses")
public class Address {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long userId;
  public String name;
  public String phone;
  public String line1;
  public String line2;
  public String city;
  public String state;
  public String pin;
}
