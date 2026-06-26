package com.ss.imagedetection.exception;

import com.ss.imagedetection.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapImageNotFoundToNotFound() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(new ImageNotFoundException(7L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Image not found for id: 7", response.getBody().details().getFirst());
    }

    @Test
    void shouldMapIllegalArgumentToBadRequest() {
        ResponseEntity<ApiErrorResponse> response = handler.handleBadRequest(new IllegalArgumentException("bad input"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("bad input", response.getBody().details().getFirst());
    }

    @Test
    void shouldMapUpstreamServiceExceptionToContainedStatus() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUpstreamService(
                new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "upstream failed")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(502, response.getBody().status());
        assertEquals("upstream failed", response.getBody().details().getFirst());
    }

    @Test
    void shouldMapGenericExceptionToInternalServerError() {
        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An unexpected error occurred", response.getBody().details().getFirst());
    }
}