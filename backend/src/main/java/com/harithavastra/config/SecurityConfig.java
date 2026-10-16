package com.harithavastra.config;

import com.harithavastra.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder encoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityFilterChain security(
    HttpSecurity http,
    AuthService auth,
    @Value("${app.allowed-origins}") String origins
  ) throws Exception {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(Arrays.asList(origins.split(",")));
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(
      List.of(
        "Content-Type",
        "Authorization",
        "X-Guest-Token",
        "Idempotency-Key"
      )
    );
    cors.setAllowCredentials(false);
    UrlBasedCorsConfigurationSource source =
      new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    http
      .csrf(c -> c.disable())
      .cors(c -> c.configurationSource(source))
      .sessionManagement(s ->
        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authorizeHttpRequests(a ->
        a
          .requestMatchers("/api/admin/**")
          .hasRole("ADMIN")
          .requestMatchers("/api/account/**")
          .authenticated()
          .anyRequest()
          .permitAll()
      )
      .exceptionHandling(e ->
        e
          .authenticationEntryPoint((r, s, x) -> {
            s.setStatus(401);
            s.setContentType("application/json");
            s.getWriter().write("{\"message\":\"Please sign in\"}");
          })
          .accessDeniedHandler((r, s, x) -> {
            s.setStatus(403);
            s.setContentType("application/json");
            s.getWriter().write(
              "{\"message\":\"Administrator access required\"}"
            );
          })
      )
      .addFilterBefore(
        new OncePerRequestFilter() {
          protected void doFilterInternal(
            HttpServletRequest r,
            HttpServletResponse s,
            FilterChain chain
          ) throws ServletException, IOException {
            String h = r.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) auth
              .authenticate(h.substring(7))
              .ifPresent(u ->
                SecurityContextHolder.getContext().setAuthentication(
                  new UsernamePasswordAuthenticationToken(
                    u.id,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + u.role))
                  )
                )
              );
            chain.doFilter(r, s);
          }
        },
        UsernamePasswordAuthenticationFilter.class
      );
    return http.build();
  }
}
