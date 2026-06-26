package com.heb.imagedetection.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigurationTest {

    @Test
    void shouldCreateExpectedOpenApiMetadata() {
        OpenAPI openAPI = new OpenApiConfiguration().imageDetectionOpenApi();

        assertEquals("Image Object Detection API", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
        assertEquals(
                "Spring Boot API for ingesting images, detecting objects, and searching image metadata.",
                openAPI.getInfo().getDescription()
        );
    }
}