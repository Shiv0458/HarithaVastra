package com.harithavastra;

import static org.assertj.core.api.Assertions.*;

import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers(disabledWithoutDocker = true)
class MySqlMigrationTest {

  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

  @Test
  void realMySqlMigrationsAndStockConstraint() throws Exception {
    Flyway.configure()
      .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
      .locations("classpath:db/migration", "classpath:db/demo")
      .load()
      .migrate();
    try (
      Connection c = DriverManager.getConnection(
        mysql.getJdbcUrl(),
        mysql.getUsername(),
        mysql.getPassword()
      );
      Statement s = c.createStatement()
    ) {
      try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM products")) {
        rs.next();
        assertThat(rs.getInt(1)).isEqualTo(4);
      }
      assertThatThrownBy(() ->
        s.executeUpdate("UPDATE products SET stock=-1 WHERE id=1")
      ).isInstanceOf(SQLException.class);
    }
  }
}
