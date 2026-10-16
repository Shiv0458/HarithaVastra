package com.harithavastra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

public class Requests {

  public record Register(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 10, max = 72) String password
  ) {}

  public record Login(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 72) String password
  ) {}

  public record Profile(
    @NotBlank @Size(max = 100) String name,
    @Pattern(regexp = "^$|[0-9+ -]{8,20}") String phone
  ) {}

  public record Delivery(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Pattern(regexp = "[6-9][0-9]{9}") String phone,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 255) String line1,
    @Size(max = 255) String line2,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String state,
    @NotBlank @Pattern(regexp = "[1-9][0-9]{5}") String pin
  ) {}

  public record Quantity(@Min(0) @Max(10) int quantity) {}

  public record Checkout(@NotNull @Valid Delivery delivery) {}

  public record Verify(
    @NotBlank String razorpay_order_id,
    @NotBlank String razorpay_payment_id,
    @NotBlank String razorpay_signature
  ) {}

  public record ImageInput(
    @NotBlank @Size(max = 1000) String url,
    @NotBlank @Size(max = 100) String label
  ) {}

  public record ProductInput(
    @NotBlank @Size(max = 200) String name,
    Long categoryId,
    @NotBlank @Size(max = 100) String collection,
    @Size(max = 60) String occasion,
    @Size(max = 60) String colour,
    @NotBlank @Size(max = 4000) String description,
    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    BigDecimal price,
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    BigDecimal originalPrice,
    @Min(0) int stock,
    boolean demo,
    @NotNull @Size(max = 25) Map<String, String> specifications,
    @NotNull @Size(max = 12) List<@Valid ImageInput> images
  ) {}

  public record CategoryInput(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Pattern(regexp = "[a-z0-9-]{1,100}") String slug
  ) {}

  public record Fulfillment(
    @Pattern(regexp = "UNFULFILLED|PACKING|SHIPPED|DELIVERED") String status
  ) {}

  public record ReviewInput(
    @Min(1) @Max(5) int rating,
    @NotBlank @Size(max = 2000) String text
  ) {}
}
