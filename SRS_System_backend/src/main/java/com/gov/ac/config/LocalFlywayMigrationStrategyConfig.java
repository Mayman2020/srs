package com.gov.ac.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Local profile only: run {@link Flyway#repair()} then {@link Flyway#migrate()}.
 *
 * <p><strong>Self-heal (local only):</strong> if {@code srs_system.flyway_schema_history} reports
 * successful migrations but {@code srs_system.app_user} is missing (drift: history without DDL), we
 * run the same steps as {@code docs/db/srs_system_full_clean_reset.sql} and re-apply migrations.
 * This matches your logs: {@code Current version: 2}, {@code 0 pending}, {@code app_user} nowhere.
 *
 * <p>Staging/production use the default Flyway strategy (no self-heal).
 */
@Configuration
@Profile("local")
public class LocalFlywayMigrationStrategyConfig {

  private static final Logger log = LoggerFactory.getLogger(LocalFlywayMigrationStrategyConfig.class);

  private final DataSource dataSource;

  public LocalFlywayMigrationStrategyConfig(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Bean
  public FlywayMigrationStrategy flywayMigrationStrategy() {
    return flyway -> {
      healCorruptHistoryWithoutTables();
      MigrationInfo[] pending = flyway.info().pending();
      log.info("Flyway (local): {} pending migration(s) before repair/migrate", pending.length);
      flyway.repair();
      MigrateResult result = flyway.migrate();
      int applied = result != null ? result.migrationsExecuted : 0;
      log.info(
          "Flyway (local): migrate finished; {} migration(s) executed this run.",
          applied);
    };
  }

  /**
   * When Flyway history claims success but the schema has no application tables (e.g. schema
   * dropped manually, or only history left), pending=0 and migrate does nothing. Drop and recreate
   * {@code srs_system} and clear {@code public.flyway_schema_history} so V1/V2 run again.
   */
  private void healCorruptHistoryWithoutTables() {
    try (Connection c = dataSource.getConnection()) {
      c.setAutoCommit(true);
      if (!schemaExists(c, "srs_system")) {
        return;
      }
      if (tableExists(c, "srs_system", "app_user")) {
        return;
      }
      if (!tableExists(c, "srs_system", "flyway_schema_history")) {
        return;
      }
      int historyRows = countFlywayHistoryRows(c);
      if (historyRows <= 0) {
        return;
      }
      log.warn(
          "Flyway (local): corrupt state - {} row(s) in flyway_schema_history but srs_system.app_user is missing. "
              + "Applying same reset as docs/db/srs_system_full_clean_reset.sql, then migrations will re-run.",
          historyRows);
      try (Statement st = c.createStatement()) {
        st.execute("DROP SCHEMA IF EXISTS srs_system CASCADE");
        st.execute("CREATE SCHEMA srs_system");
        st.execute("DROP TABLE IF EXISTS public.flyway_schema_history");
      }
      log.warn(
          "Flyway (local): schema reset complete. Next migrate() will apply V1 and V2 from scratch.");
    } catch (SQLException e) {
      throw new IllegalStateException(
          "Local Flyway self-heal failed. Run docs/db/srs_system_full_clean_reset.sql manually on this database.",
          e);
    }
  }

  private static boolean schemaExists(Connection c, String name) throws SQLException {
    try (Statement st = c.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT 1 FROM pg_namespace WHERE nspname = '" + name + "'")) {
      return rs.next();
    }
  }

  private static boolean tableExists(Connection c, String schema, String table) throws SQLException {
    try (Statement st = c.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = '"
                    + schema
                    + "' AND table_name = '"
                    + table
                    + "'")) {
      return rs.next();
    }
  }

  private static int countFlywayHistoryRows(Connection c) throws SQLException {
    try (Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM srs_system.flyway_schema_history")) {
      if (rs.next()) {
        return rs.getInt(1);
      }
      return 0;
    }
  }
}
