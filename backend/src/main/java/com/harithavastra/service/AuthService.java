package com.harithavastra.service;

import com.harithavastra.dto.Requests.*;
import com.harithavastra.model.*;
import com.harithavastra.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserAccountRepository users;
  private final AuthSessionRepository sessions;
  private final PasswordEncoder encoder;

  public AuthService(
    UserAccountRepository u,
    AuthSessionRepository s,
    PasswordEncoder p
  ) {
    users = u;
    sessions = s;
    encoder = p;
  }

  public static String hash(String value) {
    try {
      return HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(
          value.getBytes(StandardCharsets.UTF_8)
        )
      );
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static String token() {
    byte[] b = new byte[32];
    new SecureRandom().nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  public Map<String, Object> view(UserAccount u) {
    return Map.of(
      "id",
      u.id,
      "name",
      u.name,
      "email",
      u.email,
      "phone",
      Objects.toString(u.phone, ""),
      "role",
      u.role
    );
  }

  @Transactional
  public UserAccount register(Register r) {
    String email = r.email().trim().toLowerCase(Locale.ROOT);
    if (users.findByEmail(email).isPresent()) throw Errors.conflict(
      "Email already registered"
    );
    UserAccount u = new UserAccount();
    u.name = r.name().trim();
    u.email = email;
    u.passwordHash = encoder.encode(r.password());
    u.role = "CUSTOMER";
    return users.save(u);
  }

  public UserAccount login(Login r) {
    UserAccount u = users
      .findByEmail(r.email().trim().toLowerCase(Locale.ROOT))
      .orElseThrow(Errors::unauthorized);
    if (
      !encoder.matches(r.password(), u.passwordHash)
    ) throw Errors.unauthorized();
    return u;
  }

  public Map<String, Object> issue(UserAccount u) {
    String token = token();
    AuthSession s = new AuthSession();
    s.tokenHash = hash(token);
    s.userId = u.id;
    s.expiresAt = Instant.now().plus(Duration.ofHours(12));
    sessions.save(s);
    return Map.of("token", token, "user", view(u));
  }

  public Optional<UserAccount> authenticate(String token) {
    return sessions
      .findById(hash(token))
      .filter(s -> s.expiresAt.isAfter(Instant.now()))
      .flatMap(s -> users.findById(s.userId));
  }

  public void revoke(String token) {
    sessions.deleteById(hash(token));
  }

  public UserAccount user(Long id) {
    return users.findById(id).orElseThrow(Errors::unauthorized);
  }

  public Map<String, Object> update(Long id, Profile p) {
    UserAccount u = user(id);
    u.name = p.name();
    u.phone = p.phone();
    users.save(u);
    return view(u);
  }
}
