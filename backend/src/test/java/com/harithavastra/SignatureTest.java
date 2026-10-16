package com.harithavastra;

import static org.assertj.core.api.Assertions.*;

import com.harithavastra.service.Signatures;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SignatureTest {

  @Test
  void knownHmacVector() {
    byte[] body = "The quick brown fox jumps over the lazy dog".getBytes(
      StandardCharsets.UTF_8
    );
    assertThat(Signatures.sign("key", body)).isEqualTo(
      "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8"
    );
  }

  @Test
  void tamperingAndMalformedSignaturesFail() {
    byte[] body = "order_A|pay_A".getBytes(StandardCharsets.UTF_8);
    String sig = Signatures.sign("secret", body);
    assertThat(Signatures.valid("secret", body, sig)).isTrue();
    assertThat(
      Signatures.valid(
        "secret",
        "order_B|pay_A".getBytes(StandardCharsets.UTF_8),
        sig
      )
    ).isFalse();
    assertThat(Signatures.valid("other", body, sig)).isFalse();
    assertThat(Signatures.valid("secret", body, "zz")).isFalse();
    assertThat(Signatures.valid("", body, sig)).isFalse();
  }
}
