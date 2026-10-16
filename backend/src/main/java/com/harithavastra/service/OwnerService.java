package com.harithavastra.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class OwnerService {

  public String owner(Authentication auth, HttpServletRequest req) {
    if (auth != null && auth.getPrincipal() instanceof Long id) return (
      "u:" + id
    );
    return guest(req);
  }

  public String guest(HttpServletRequest req) {
    String key = req.getHeader("X-Guest-Token");
    if (key == null || !key.matches("[a-f0-9-]{36}")) throw Errors.bad(
      "A valid guest token is required"
    );
    try {
      java.util.UUID.fromString(key);
    } catch (Exception e) {
      throw Errors.bad("Invalid guest token");
    }
    return "g:" + key;
  }

  public Long userId(Authentication a) {
    if (
      a == null || !(a.getPrincipal() instanceof Long)
    ) throw Errors.unauthorized();
    return (Long) a.getPrincipal();
  }
}
