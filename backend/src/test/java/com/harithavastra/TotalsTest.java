package com.harithavastra;

import static org.assertj.core.api.Assertions.*;

import com.harithavastra.service.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TotalsTest {

  @Test
  void calculatesDiscountShippingAndRoundedTax() {
    var result = Totals.calculate(
      List.of(
        new Totals.Line(new BigDecimal("100.05"), new BigDecimal("120.00"), 3)
      ),
      new BigDecimal("40"),
      new BigDecimal("0.05")
    );
    assertThat(result.subtotal()).isEqualByComparingTo("360.00");
    assertThat(result.discount()).isEqualByComparingTo("59.85");
    assertThat(result.tax()).isEqualByComparingTo("15.01");
    assertThat(result.total()).isEqualByComparingTo("355.16");
    assertThat(Totals.paise(result.total())).isEqualTo(35516);
  }

  @Test
  void noFakeDiscountAndRejectsInvalidQuantity() {
    var r = Totals.calculate(
      List.of(new Totals.Line(new BigDecimal("100"), new BigDecimal("90"), 1)),
      BigDecimal.ZERO,
      BigDecimal.ZERO
    );
    assertThat(r.discount()).isEqualByComparingTo("0");
    assertThatThrownBy(() ->
      Totals.calculate(
        List.of(new Totals.Line(BigDecimal.ONE, null, 0)),
        BigDecimal.ZERO,
        BigDecimal.ZERO
      )
    ).isInstanceOf(
      org.springframework.web.server.ResponseStatusException.class
    );
  }

  @Test
  void emptyCartRejected() {
    assertThatThrownBy(() ->
      Totals.calculate(List.of(), BigDecimal.ZERO, BigDecimal.ZERO)
    ).isInstanceOf(
      org.springframework.web.server.ResponseStatusException.class
    );
  }
}
