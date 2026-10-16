package com.harithavastra.service;

import com.harithavastra.repository.*;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationJob {

  private final ShopOrderRepository orders;
  private final PaymentService payments;
  private final AuthSessionRepository sessions;

  public ReservationJob(
    ShopOrderRepository o,
    PaymentService p,
    AuthSessionRepository s
  ) {
    orders = o;
    payments = p;
    sessions = s;
  }

  @Scheduled(fixedDelay = 60000)
  public void expire() {
    for (var order : orders.findByStatusInAndExpiresAtBefore(
      List.of("CREATED", "PAYMENT_PENDING", "GATEWAY_ERROR"),
      Instant.now()
    ))
      try {
        payments.reconcile(order.id, true);
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(getClass()).warn(
          "Could not reconcile order {}; reservation retained",
          order.id
        );
      }
  }
}
