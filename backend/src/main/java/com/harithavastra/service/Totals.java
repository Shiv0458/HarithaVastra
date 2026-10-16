package com.harithavastra.service;

import java.math.*;
import java.util.*;

public class Totals {

  public record Line(
    BigDecimal price,
    BigDecimal originalPrice,
    int quantity
  ) {}

  public record Result(
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal shipping,
    BigDecimal tax,
    BigDecimal total
  ) {}

  public static Result calculate(
    List<Line> lines,
    BigDecimal shipping,
    BigDecimal rate
  ) {
    if (lines.isEmpty()) throw Errors.bad("Your bag is empty");
    BigDecimal subtotal = BigDecimal.ZERO,
      discount = BigDecimal.ZERO;
    for (Line l : lines) {
      if (
        l.quantity() < 1 || l.quantity() > 10 || l.price().signum() <= 0
      ) throw Errors.bad("Invalid cart item");
      BigDecimal original =
        l.originalPrice() != null && l.originalPrice().compareTo(l.price()) > 0
          ? l.originalPrice()
          : l.price();
      subtotal = subtotal.add(
        original.multiply(BigDecimal.valueOf(l.quantity()))
      );
      discount = discount.add(
        original.subtract(l.price()).multiply(BigDecimal.valueOf(l.quantity()))
      );
    }
    BigDecimal taxable = subtotal.subtract(discount);
    BigDecimal tax = taxable.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    return new Result(
      subtotal.setScale(2),
      discount.setScale(2),
      shipping.setScale(2),
      tax,
      taxable.add(shipping).add(tax).setScale(2)
    );
  }

  public static long paise(BigDecimal amount) {
    return amount.movePointRight(2).longValueExact();
  }
}
