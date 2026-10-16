package com.harithavastra.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface PaymentGateway {
  boolean configured();
  String keyId();
  String mode();
  String createOrder(String receipt, long amount);
  JsonNode payment(String id);
  JsonNode payments(String orderId);
  boolean verify(String orderId, String paymentId, String signature);
  boolean verifyWebhook(byte[] body, String signature);
}
