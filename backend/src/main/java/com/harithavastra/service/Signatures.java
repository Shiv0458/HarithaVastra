package com.harithavastra.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Signatures {

  public static String sign(String secret, byte[] body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(
        new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
      );
      return HexFormat.of().formatHex(mac.doFinal(body));
    } catch (Exception e) {
      throw new IllegalStateException("Signature configuration unavailable", e);
    }
  }

  public static boolean valid(String secret, byte[] body, String signature) {
    if (
      secret == null ||
      secret.isBlank() ||
      signature == null ||
      !signature.matches("[a-fA-F0-9]{64}")
    ) return false;
    return MessageDigest.isEqual(
      HexFormat.of().parseHex(sign(secret, body)),
      HexFormat.of().parseHex(signature)
    );
  }
}
