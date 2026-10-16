package com.harithavastra.controller;

import com.harithavastra.dto.Requests.*;
import com.harithavastra.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService auth;
  private final CartService carts;
  private final OwnerService owners;

  public AuthController(AuthService a, CartService c, OwnerService o) {
    auth = a;
    carts = c;
    owners = o;
  }

  private Map<String, Object> complete(
    com.harithavastra.model.UserAccount u,
    HttpServletRequest req
  ) {
    String user = "u:" + u.id,
      guest = owners.guest(req);
    carts.ensure(user);
    carts.ensure(guest);
    carts.merge(guest, user);
    return auth.issue(u);
  }

  @PostMapping("/register")
  Object register(@Valid @RequestBody Register r, HttpServletRequest req) {
    return complete(auth.register(r), req);
  }

  @PostMapping("/login")
  Object login(@Valid @RequestBody Login r, HttpServletRequest req) {
    return complete(auth.login(r), req);
  }

  @PostMapping("/logout")
  Object logout(
    @RequestHeader(value = "Authorization", defaultValue = "") String h
  ) {
    if (h.startsWith("Bearer ")) auth.revoke(h.substring(7));
    return Map.of("ok", true);
  }
}
