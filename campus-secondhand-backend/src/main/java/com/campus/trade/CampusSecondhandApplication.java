package com.campus.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

import java.util.List;

@SpringBootApplication
@MapperScan("com.campus.trade.mapper")
@EnableScheduling
public class CampusSecondhandApplication {

    private static final List<String> SENSITIVE_DEFAULT_VALUES = List.of(
            "YOUR_MYSQL_USERNAME",
            "YOUR_MYSQL_PASSWORD",
            "CHANGE_ME_CHANGE_ME_CHANGE_ME_CHANGE_ME_CHANGE_ME_32_BYTES_MIN",
            "CHANGE_ME_SHARE_GATE_PASSPHRASE"
    );

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CampusSecondhandApplication.class);
        application.addListeners(new SensitiveConfigGuard());
        application.run(args);
    }

    static void validateSensitiveConfig(Environment environment) {
        validate("spring.datasource.username", environment.getProperty("spring.datasource.username"));
        validate("spring.datasource.password", environment.getProperty("spring.datasource.password"));
        validate("app.jwt.secret", environment.getProperty("app.jwt.secret"));
        validate("app.share-gate.passphrase", environment.getProperty("app.share-gate.passphrase"));
    }

    private static void validate(String key, String value) {
        if (!StringUtils.hasText(value) || SENSITIVE_DEFAULT_VALUES.contains(value.trim())) {
            throw new IllegalStateException("Refusing to start with insecure default config: " + key);
        }
    }

    static class SensitiveConfigGuard implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

        @Override
        public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
            validateSensitiveConfig(event.getEnvironment());
        }
    }
}
