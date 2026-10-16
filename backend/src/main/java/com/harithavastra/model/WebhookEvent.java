package com.harithavastra.model;

import jakarta.persistence.*;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

  @Id
  public String id;

  public java.time.Instant processedAt;
}
