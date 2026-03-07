package com.campus.trade.common;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void shouldHideConstraintViolationDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ApiResponse<String> response = handler.handleConstraintViolation(new ConstraintViolationException("internal detail", null));

        assertEquals(400, response.getCode());
        assertEquals("Validation failed", response.getMessage());
    }

    @Test
    void shouldHideUnhandledExceptionDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ApiResponse<Void> response = handler.handleOther(new RuntimeException("jdbc password leaked"));

        assertEquals(500, response.getCode());
        assertEquals("Internal server error", response.getMessage());
    }
}
