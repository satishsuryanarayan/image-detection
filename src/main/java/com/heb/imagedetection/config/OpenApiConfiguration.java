package com.heb.imagedetection.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the OpenAPI metadata displayed by Swagger UI and exposed through /v3/api-docs.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI imageDetectionOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Image Object Detection API")
                        .version("1.0.0")
                        .description("Spring Boot API for ingesting images, detecting objects, and searching image metadata."));
    }
}