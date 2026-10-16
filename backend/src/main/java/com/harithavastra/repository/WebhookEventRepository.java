package com.harithavastra.repository;

import com.harithavastra.model.WebhookEvent;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WebhookEventRepository
  extends JpaRepository<WebhookEvent, String> {}
