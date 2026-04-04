package com.gov.ac.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensures the target PostgreSQL database exists before DataSource / Flyway
 * initialise.  Connects to the default {@code postgres} catalogue using the
 * same credentials configured in {@code spring.datasource.*} and runs
 * {@code CREATE DATABASE} when the target database is missing.
 *
 * <p>Bean ordering is enforced via a {@link BeanFactoryPostProcessor} that
 * makes {@code dataSource}, {@code flyway} and {@code flywayInitializer}
 * depend on the {@code databaseCreator} bean.</p>
 */
@Configuration
class DatabaseAutoCreation {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAutoCreation.class);

    /**
     * Adds an explicit {@code dependsOn("databaseCreator")} to every bean
     * that needs the database to exist, guaranteeing creation runs first.
     */
    @Bean
    static BeanFactoryPostProcessor databaseCreationEnforcer() {
        return factory -> {
            for (String name : List.of("dataSource", "flyway", "flywayInitializer")) {
                if (factory.containsBeanDefinition(name)) {
                    BeanDefinition def = factory.getBeanDefinition(name);
                    List<String> deps = def.getDependsOn() != null
                            ? new ArrayList<>(List.of(def.getDependsOn()))
                            : new ArrayList<>();
                    if (!deps.contains("databaseCreator")) {
                        deps.add("databaseCreator");
                        def.setDependsOn(deps.toArray(String[]::new));
                    }
                }
            }
        };
    }

    /**
     * Factory method whose side-effect creates the database.  Runs before
     * any bean that declares {@code @DependsOn("databaseCreator")}.
     */
    @Bean(name = "databaseCreator")
    Object databaseCreator(DataSourceProperties dsProps) {
        String url  = dsProps.determineUrl();
        String user = dsProps.determineUsername();
        String pass = dsProps.determinePassword();

        String dbName = extractDbName(url);
        if (dbName == null || dbName.isBlank()) {
            log.warn("Could not extract database name from JDBC URL – skipping auto-creation.");
            return Boolean.TRUE;
        }

        String adminUrl = replaceDbInUrl(url, dbName, "postgres");

        try (Connection conn = DriverManager.getConnection(adminUrl, user, pass)) {
            if (databaseExists(conn, dbName)) {
                log.info("Database '{}' already exists.", dbName);
            } else {
                createDatabase(conn, dbName);
                log.info("Database '{}' created.", dbName);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Failed to ensure database '" + dbName + "' exists: " + ex.getMessage(), ex);
        }

        return Boolean.TRUE;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static boolean databaseExists(Connection conn, String dbName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM pg_database WHERE datname = ?")) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void createDatabase(Connection conn, String dbName) throws SQLException {
        if (!dbName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid database name: " + dbName);
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE \"" + dbName + "\"");
        }
    }

    /** {@code jdbc:postgresql://host:port/mydb?params → mydb} */
    private static String extractDbName(String url) {
        if (url == null) return null;
        String noParams = url.split("\\?")[0];
        int slash = noParams.lastIndexOf('/');
        return slash >= 0 ? noParams.substring(slash + 1) : null;
    }

    /** Replace just the database segment in a JDBC URL, preserving query params. */
    private static String replaceDbInUrl(String url, String oldDb, String newDb) {
        int i = url.lastIndexOf("/" + oldDb);
        return url.substring(0, i + 1) + newDb + url.substring(i + 1 + oldDb.length());
    }
}
