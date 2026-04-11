package com.gov.ac.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway callbacks: (1) log the session Flyway uses for migrations (same connection family as DDL), (2)
 * after migrate, verify core tables exist in {@code srs_system}.
 *
 * <p><strong>Root cause addressed:</strong> Hikari {@code connection-init-sql} does not always apply
 * to the JDBC connection Flyway uses for {@code V1}/{@code V2}. Unqualified {@code CREATE TABLE
 * app_user} then lands in {@code public} while this check looks in {@code srs_system}. Configure
 * {@code spring.flyway.init-sql} in {@code application.yml} so Flyway sessions set {@code
 * search_path} before scripts run (belt-and-suspenders with {@code SET search_path} inside V1 and
 * {@code spring.flyway.init-sqls}).
 */
@Configuration
public class FlywayPostMigrateSchemaVerificationConfig {

  private static final Logger log = LoggerFactory.getLogger(FlywayPostMigrateSchemaVerificationConfig.class);

  @Bean
  FlywayConfigurationCustomizer srsFlywaySessionAndVerifyCallbacks() {
    return configuration ->
        configuration.callbacks(
            new Callback() {

              @Override
              public boolean supports(Event event, Context context) {
                return Event.BEFORE_MIGRATE.equals(event);
              }

              @Override
              public boolean canHandleInTransaction(Event event, Context context) {
                return false;
              }

              @Override
              public void handle(Event event, Context context) {
                logFlywaySession(context.getConnection());
              }

              @Override
              public String getCallbackName() {
                return "SrsFlywaySessionLogger";
              }
            },
            new Callback() {

              @Override
              public boolean supports(Event event, Context context) {
                return Event.AFTER_MIGRATE.equals(event);
              }

              @Override
              public boolean canHandleInTransaction(Event event, Context context) {
                return false;
              }

              @Override
              public void handle(Event event, Context context) {
                verifyRequiredTables(context.getConnection());
              }

              @Override
              public String getCallbackName() {
                return "SrsVerifyBaselineAndCamundaAfterMigrate";
              }
            });
  }

  private static void logFlywaySession(Connection connection) {
    try (Statement st = connection.createStatement()) {
      String db = null;
      String schema = null;
      String searchPath = null;
      try (ResultSet rs =
          st.executeQuery("SELECT current_database(), current_schema()")) {
        if (rs.next()) {
          db = rs.getString(1);
          schema = rs.getString(2);
        }
      }
      try (ResultSet rs = st.executeQuery("SHOW search_path")) {
        if (rs.next()) {
          searchPath = rs.getString(1);
        }
      }
      log.info(
          "Flyway migration session: current_database={} current_schema={} search_path={}",
          db,
          schema,
          searchPath);
    } catch (SQLException e) {
      log.warn("Flyway session diagnostic query failed: {}", e.getMessage());
    }
  }

  private static void verifyRequiredTables(Connection connection) {
    if (!tableExistsInSchema(connection, "srs_system", "app_user")) {
      String locations = findTableLocations(connection, "app_user");
      throw new IllegalStateException(
          "Flyway reported success but srs_system.app_user is missing. "
              + "app_user tables found (schema-qualified): ["
              + locations
              + "]. "
              + "If tables exist only in public, Flyway's migration connection did not use "
              + "search_path srs_system (ensure spring.flyway.init-sqls includes SET search_path). "
              + "If none exist, Flyway history may claim versions applied without tables (local profile "
              + "auto-resets this on next start; otherwise run docs/db/srs_system_full_clean_reset.sql).");
    }
    if (!tableExistsInSchema(connection, "srs_system", "act_ge_property")) {
      throw new IllegalStateException(
          "Flyway reported success but srs_system.act_ge_property (Camunda) is missing. "
              + "Ensure V2__camunda_schema.sql is on the classpath. "
              + "If the DB previously used Camunda auto-DDL, resolve conflicts or reset with "
              + "docs/db/srs_system_full_clean_reset.sql, then restart.");
    }
  }

  private static boolean tableExistsInSchema(Connection c, String schema, String table) {
    try (Statement st = c.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT 1 FROM information_schema.tables "
                    + "WHERE table_schema = '"
                    + schema
                    + "' AND table_name = '"
                    + table
                    + "'")) {
      return rs.next();
    } catch (SQLException e) {
      throw new IllegalStateException("Post-Flyway schema verification failed", e);
    }
  }

  /** Comma-separated schema.table for debugging (empty string if none). */
  private static String findTableLocations(Connection c, String tableName) {
    try (Statement st = c.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT table_schema || '.' || table_name FROM information_schema.tables "
                    + "WHERE table_name = '"
                    + tableName
                    + "' AND table_type = 'BASE TABLE' "
                    + "AND table_schema NOT IN ('pg_catalog','information_schema') "
                    + "ORDER BY table_schema")) {
      StringBuilder sb = new StringBuilder();
      while (rs.next()) {
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(rs.getString(1));
      }
      return sb.length() == 0 ? "(none)" : sb.toString();
    } catch (SQLException e) {
      return "(query failed: " + e.getMessage() + ")";
    }
  }
}
