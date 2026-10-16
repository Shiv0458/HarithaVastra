package com.harithavastra.repository;

import com.harithavastra.model.ShopOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, String> {
  java.util.Optional<ShopOrder> findByOwnerKeyAndIdempotencyKey(
    String owner,
    String key
  );
  java.util.List<ShopOrder> findByOwnerKeyOrderByCreatedAtDesc(String owner);
  java.util.List<ShopOrder> findByStatusInAndExpiresAtBefore(
    java.util.List<String> statuses,
    java.time.Instant now
  );
  java.util.Optional<ShopOrder> findByGatewayOrderId(String id);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from ShopOrder o where o.id=:id")
  java.util.Optional<ShopOrder> lock(@Param("id") String id);
}
