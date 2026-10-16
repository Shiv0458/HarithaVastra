package com.harithavastra.repository;

import com.harithavastra.model.ProductImage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository
  extends JpaRepository<ProductImage, Long>
{
  java.util.List<ProductImage> findByProductIdOrderByPositionAsc(Long id);
  void deleteByProductId(Long id);
}
