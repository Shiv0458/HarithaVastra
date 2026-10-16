package com.harithavastra.repository;

import com.harithavastra.model.WishlistItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WishlistItemRepository
  extends JpaRepository<WishlistItem, Long>
{
  java.util.List<WishlistItem> findByOwnerKey(String owner);
  java.util.Optional<WishlistItem> findByOwnerKeyAndProductId(
    String owner,
    Long id
  );
  void deleteByOwnerKey(String owner);
}
