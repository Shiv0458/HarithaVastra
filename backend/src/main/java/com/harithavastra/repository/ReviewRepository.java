package com.harithavastra.repository;

import com.harithavastra.model.Review;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  java.util.List<Review> findByProductIdOrderByCreatedAtDesc(Long id);
  boolean existsByProductIdAndUserId(Long productId, Long userId);
}
