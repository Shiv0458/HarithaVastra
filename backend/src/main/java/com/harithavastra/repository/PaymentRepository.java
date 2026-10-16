package com.harithavastra.repository;

import com.harithavastra.model.Payment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, String> {
  java.util.List<Payment> findByOrderId(String id);
}
