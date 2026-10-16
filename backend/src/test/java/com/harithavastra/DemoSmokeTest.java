package com.harithavastra;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
  properties = {
    "spring.datasource.url=jdbc:h2:mem:demo_smoke;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.flyway.locations=classpath:db/migration,classpath:db/demo",
    "app.demo=true",
    "app.admin-email=admin-fixture@example.com",
    "app.admin-password=test-only-long-password",
  }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoSmokeTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper json;

  @Test
  void demoMigrationsLoadAndUnconfiguredPaymentsStayUnavailable()
    throws Exception {
    mvc
      .perform(get("/api/config"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.demo").value(true))
      .andExpect(jsonPath("$.paymentConfigured").value(false));
    mvc
      .perform(get("/api/products"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.total").value(4))
      .andExpect(jsonPath("$.items[0].demo").value(true))
      .andExpect(jsonPath("$.items[0].reviewCount").value(0));
  }

  @Test
  void adminBootstrapLoginAndProtectedWriteWork() throws Exception {
    String guest = UUID.randomUUID().toString();
    String response = mvc
      .perform(
        post("/api/auth/login")
          .header("X-Guest-Token", guest)
          .contentType("application/json")
          .content(
            "{\"email\":\"admin-fixture@example.com\",\"password\":\"test-only-long-password\"}"
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.user.role").value("ADMIN"))
      .andReturn()
      .getResponse()
      .getContentAsString();
    String token = json.readTree(response).path("token").asText();
    mvc
      .perform(
        get("/api/admin/products").header("Authorization", "Bearer " + token)
      )
      .andExpect(status().isOk());
    mvc
      .perform(
        post("/api/admin/categories")
          .header("Authorization", "Bearer " + token)
          .contentType("application/json")
          .content("{\"name\":\"Test Category\",\"slug\":\"test-category\"}")
      )
      .andExpect(status().isOk());
    mvc
      .perform(
        post("/api/auth/logout").header("Authorization", "Bearer " + token)
      )
      .andExpect(status().isOk());
    mvc
      .perform(
        get("/api/admin/products").header("Authorization", "Bearer " + token)
      )
      .andExpect(status().isUnauthorized());
  }
}
