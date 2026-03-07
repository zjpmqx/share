package com.campus.trade;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CampusSecondhandApplicationTest {

    @Test
    void shouldRejectDefaultJwtSecret() {
        MockEnvironment environment = secureEnvironment();
        environment.setProperty("app.jwt.secret", "CHANGE_ME_CHANGE_ME_CHANGE_ME_CHANGE_ME_CHANGE_ME_32_BYTES_MIN");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> CampusSecondhandApplication.validateSensitiveConfig(environment));

        assertEquals("Refusing to start with insecure default config: app.jwt.secret", ex.getMessage());
    }

    @Test
    void shouldRejectBlankShareGatePassphrase() {
        MockEnvironment environment = secureEnvironment();
        environment.setProperty("app.share-gate.passphrase", " ");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> CampusSecondhandApplication.validateSensitiveConfig(environment));

        assertEquals("Refusing to start with insecure default config: app.share-gate.passphrase", ex.getMessage());
    }

    @Test
    void shouldAllowSecureSensitiveConfig() {
        assertDoesNotThrow(() -> CampusSecondhandApplication.validateSensitiveConfig(secureEnvironment()));
    }

    private MockEnvironment secureEnvironment() {
        return new MockEnvironment()
                .withProperty("spring.datasource.username", "campus_user")
                .withProperty("spring.datasource.password", "campus_pwd_123")
                .withProperty("app.jwt.secret", "a_secure_jwt_secret_with_more_than_32_chars")
                .withProperty("app.share-gate.passphrase", "share-gate-strong-passphrase");
    }
}
