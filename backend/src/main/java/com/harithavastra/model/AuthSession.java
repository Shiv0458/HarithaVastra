package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {

  @Id
  public String tokenHash;

  public Long userId;
  public java.time.Instant expiresAt;
}
