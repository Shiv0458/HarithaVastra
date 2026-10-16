package com.harithavastra.repository;

import com.harithavastra.model.OrderItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  java.util.List<OrderItem> findByOrderIdOrderByProductId(String id);
  boolean existsByProductIdAndOrderIdIn(
    Long productId,
    java.util.Collection<String> orders
  );
}
