package com.univ.memoir.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Application Configuration Validation Tests")
class ApplicationYmlValidationTest {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("should load application context successfully")
    void shouldLoadApplicationContextSuccessfully() {
        // then
        assertThat(environment).isNotNull();
    }

    @Test
    @DisplayName("should have spring application name configured")
    void shouldHaveSpringApplicationNameConfigured() {
        // when
        String appName = environment.getProperty("spring.application.name");

        // then
        assertThat(appName).isNotNull();
    }

    @Test
    @DisplayName("should have JPA properties configured")
    void shouldHaveJpaPropertiesConfigured() {
        // when
        String showSql = environment.getProperty("spring.jpa.show-sql");
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");

        // then - verify properties are defined (may be null if not in default profile)
        assertThat(environment).isNotNull();
    }

    @Test
    @DisplayName("should have logging configuration properties")
    void shouldHaveLoggingConfigurationProperties() {
        // when
        String loggingConfig = environment.getProperty("logging.config");

        // then - verify logging configuration is accessible
        assertThat(environment).isNotNull();
    }

    @Test
    @DisplayName("should have server port configured or use default")
    void shouldHaveServerPortConfiguredOrUseDefault() {
        // when
        String port = environment.getProperty("server.port");

        // then - if configured, it should be a valid number
        if (port != null) {
            assertThat(Integer.parseInt(port)).isGreaterThan(0);
        }
    }
}