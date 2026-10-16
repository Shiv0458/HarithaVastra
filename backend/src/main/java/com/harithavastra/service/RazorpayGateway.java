package com.harithavastra.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RazorpayGateway implements PaymentGateway {

  private final String key, secret, webhook, mode;
  private final RestClient client;

  public RazorpayGateway(
    @Value("${app.razorpay.key-id}") String key,
    @Value("${app.razorpay.key-secret}") String secret,
    @Value("${app.razorpay.webhook-secret}") String webhook,
    @Value("${app.razorpay.mode}") String mode
  ) {
    this.key = key;
    this.secret = secret;
    this.webhook = webhook;
    this.mode = mode;
    if (
      !List.of("test", "live").contains(mode)
    ) throw new IllegalArgumentException("PAYMENT_MODE must be test or live");
    if (
      !key.isBlank() && !key.startsWith("rzp_" + mode + "_")
    ) throw new IllegalArgumentException(
      "Razorpay key and PAYMENT_MODE do not match"
    );
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    );
    factory.setReadTimeout(Duration.ofSeconds(20));
    client = RestClient.builder()
      .baseUrl("https://api.razorpay.com/v1")
      .requestFactory(factory)
      .defaultHeaders(h -> h.setBasicAuth(key, secret))
      .build();
  }

  public boolean configured() {
    return !key.isBlank() && !secret.isBlank() && !webhook.isBlank();
  }

  public String keyId() {
    return key;
  }

  public String mode() {
    return mode;
  }

  public String createOrder(String receipt, long amount) {
    if (!configured()) throw Errors.conflict(
      "Payments are not configured. Add Razorpay test credentials on the server."
    );
    try {
      return client
        .post()
        .uri("/orders")
        .body(
          Map.of(
            "amount",
            amount,
            "currency",
            "INR",
            "receipt",
            receipt,
            "partial_payment",
            false
          )
        )
        .retrieve()
        .body(JsonNode.class)
        .path("id")
        .asText();
    } catch (Exception e) {
      throw Errors.conflict(
        "Payment gateway unavailable. Your order is saved; please check its status before retrying."
      );
    }
  }

  public JsonNode payment(String id) {
    if (!id.matches("pay_[A-Za-z0-9]+")) throw Errors.bad(
      "Invalid payment identifier"
    );
    return client
      .get()
      .uri("/payments/{id}", id)
      .retrieve()
      .body(JsonNode.class);
  }

  public JsonNode payments(String id) {
    return client
      .get()
      .uri("/orders/{id}/payments", id)
      .retrieve()
      .body(JsonNode.class)
      .path("items");
  }

  public boolean verify(String orderId, String paymentId, String signature) {
    return Signatures.valid(
      secret,
      (orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8),
      signature
    );
  }

  public boolean verifyWebhook(byte[] body, String signature) {
    return Signatures.valid(webhook, body, signature);
  }
}
