package com.markdownconverterpro.dto;

import java.time.Instant;

public record HealthResponse(String status, String application, String timestamp) {

    public static HealthResponse up(String application) {
        return new HealthResponse("UP", application, Instant.now().toString());
    }
}
