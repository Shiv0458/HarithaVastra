package com.harithavastra.repository;

import com.harithavastra.model.Product;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
  extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product>
{
  @Query(
    "select distinct p.colour from Product p where p.archived=false and p.colour is not null order by p.colour"
  )
  java.util.List<String> colours();

  @Query(
    "select distinct p.collection from Product p where p.archived=false and p.collection is not null order by p.collection"
  )
  java.util.List<String> collections();

  @Query(
    "select distinct p.occasion from Product p where p.archived=false and p.occasion is not null order by p.occasion"
  )
  java.util.List<String> occasions();

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Product p where p.id=:id")
  java.util.Optional<Product> lockById(@Param("id") Long id);
}
