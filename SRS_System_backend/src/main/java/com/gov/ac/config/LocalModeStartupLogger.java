package com.gov.ac.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** One-line confirmation that the JVM is using the {@code local} Spring profile. */
@Component
@Profile("local")
@Slf4j
@Order(0)
public class LocalModeStartupLogger implements ApplicationRunner {

  @Override
  public void run(ApplicationArguments args) {
    log.info(
        "Running in LOCAL mode (spring.profiles.active includes 'local'). "
            + "Swagger UI: /swagger-ui.html | Health: /actuator/health | API: /api/v1");
  }
}
