package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true)
  public String email;

  public String name;
  public String phone;
  public String passwordHash;
  public String role;
}
