package com.harithavastra.repository;

import com.harithavastra.model.Address;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, Long> {
  java.util.List<Address> findByUserId(Long userId);
}
