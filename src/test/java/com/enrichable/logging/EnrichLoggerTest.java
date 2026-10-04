package com.enrichable.logging;

import com.enrichable.EnrichableException;
import com.enrichable.config.ErrorLevel;
import com.enrichable.config.LogConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class EnrichLoggerTest {

    @Test
    void shouldAllowCustomLoggerInjection() {
        AtomicBoolean called = new AtomicBoolean();

        EnrichLogger logger = (information, thrownAt, config) -> {
            called.set(true);
            assertEquals("UserService", information.getFirst().getContext());
            assertEquals("USER_404", information.getFirst().getCode());
            assertEquals(ErrorLevel.ERROR, information.getFirst().getErrorLevel());
            assertNotNull(thrownAt);
            assertNotNull(config);
            return "test-code";
        };

        EnrichableException exception = new EnrichableException.Builder(
                "UserService",
                "User not found"
        ).code("USER_404").build();

        String code = exception.setLogger(logger).writeLog();

        assertTrue(called.get());
        assertEquals("test-code", code);
    }

    @Test
    void shouldRejectNullLogger() {
        EnrichableException exception = new EnrichableException.Builder(
                "UserService",
                "User not found"
        ).build();

        assertThrows(
                IllegalArgumentException.class,
                () -> exception.setLogger(null)
        );
    }

    @Test
    void fileLoggerShouldRemainDefaultImplementation() {
        EnrichableException exception = new EnrichableException.Builder(
                "UserService",
                "User not found"
        ).build();

        assertDoesNotThrow(() -> exception.setLogConfig(
                new LogConfig().filePath("target/enrichable-infrastructure-test.log")
        ).writeLog());
    }
}
