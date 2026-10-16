package com.harithavastra.repository;

import com.harithavastra.model.CartItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  java.util.List<CartItem> findByOwnerKeyOrderByProductId(String owner);
  java.util.Optional<CartItem> findByOwnerKeyAndProductId(
    String owner,
    Long id
  );
  void deleteByOwnerKey(String owner);
}
