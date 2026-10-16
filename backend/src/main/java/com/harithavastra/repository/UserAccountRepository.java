package com.harithavastra.repository;

import com.harithavastra.model.UserAccount;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository
  extends JpaRepository<UserAccount, Long>
{
  java.util.Optional<UserAccount> findByEmail(String email);
}
