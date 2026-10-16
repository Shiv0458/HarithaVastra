package com.harithavastra.repository;

import com.harithavastra.model.Cart;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, String> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Cart c where c.ownerKey=:owner")
  java.util.Optional<Cart> lock(@Param("owner") String owner);
}
