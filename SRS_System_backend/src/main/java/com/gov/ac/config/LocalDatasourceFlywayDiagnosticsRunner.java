package com.gov.ac.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Local profile: logs resolved datasource/Flyway settings and a JDBC sanity query after startup so
 * you can prove the app, Flyway, and ad-hoc SQL (e.g. reset script) target the same catalog/schema.
 */
@Component
@Profile("local")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class LocalDatasourceFlywayDiagnosticsRunner implements ApplicationRunner {

  private final Environment environment;
  private final DataSource dataSource;
  private final FlywayProperties flywayProperties;

  @Override
  public void run(ApplicationArguments args) throws SQLException {
    log.info(
        "Datasource (resolved env): url={} username={}",
        environment.getProperty("spring.datasource.url"),
        environment.getProperty("spring.datasource.username"));
    log.info(
        "Flyway (resolved env): locations={} schemas={} default-schema={} init-sqls={}",
        environment.getProperty("spring.flyway.locations"),
        environment.getProperty("spring.flyway.schemas"),
        environment.getProperty("spring.flyway.default-schema"),
        flywayProperties.getInitSqls());

    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT current_database(), current_schema(), current_setting('search_path', true)")) {
      if (rs.next()) {
        log.info(
            "Pool JDBC session (post-Flyway): current_database={} current_schema={} search_path={}",
            rs.getString(1),
            rs.getString(2),
            rs.getString(3));
      }
    }
  }
}
