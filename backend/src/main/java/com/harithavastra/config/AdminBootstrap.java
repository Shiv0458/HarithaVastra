package com.harithavastra.config;

import com.harithavastra.model.UserAccount;
import com.harithavastra.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {

  private final UserAccountRepository users;
  private final PasswordEncoder encoder;
  private final String email, password;

  public AdminBootstrap(
    UserAccountRepository u,
    PasswordEncoder p,
    @Value("${app.admin-email}") String e,
    @Value("${app.admin-password}") String pw
  ) {
    users = u;
    encoder = p;
    email = e;
    password = pw;
  }

  public void run(String... args) {
    if (email.isBlank() || password.isBlank()) return;
    if (
      password.length() < 12 || password.length() > 72
    ) throw new IllegalArgumentException(
      "ADMIN_PASSWORD must have 12–72 characters"
    );
    String normalized = email.toLowerCase(java.util.Locale.ROOT).trim();
    if (users.findByEmail(normalized).isPresent()) return;
    UserAccount u = new UserAccount();
    u.name = "Haritha Vastra Admin";
    u.email = normalized;
    u.passwordHash = encoder.encode(password);
    u.role = "ADMIN";
    users.save(u);
  }
}
