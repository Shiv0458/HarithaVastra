package com.harithavastra.repository;

import com.harithavastra.model.AuthSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository
  extends JpaRepository<AuthSession, String>
{
  void deleteByExpiresAtBefore(java.time.Instant time);
}
