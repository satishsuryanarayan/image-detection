package com.ss.imagedetection.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ss.imagedetection.detector.ClarifaiObjectDetectionService;
import com.ss.imagedetection.detector.ImaggaObjectDetectionService;
import com.ss.imagedetection.detector.MockObjectDetectionService;
import com.ss.imagedetection.detector.ObjectDetectionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DetectorConfigurationTest {

    private final DetectorConfiguration configuration = new DetectorConfiguration();

    @Test
    void shouldUseMockDetectorWhenProviderIsMock() {
        ObjectDetectionService service = configuration.objectDetectionService("mock", "", "", 70, -1, 1, 2, "", "clarifai", "main", "general-image-recognition", 0.70, 1, 5, new ObjectMapper());

        assertInstanceOf(MockObjectDetectionService.class, service);
    }

    @Test
    void shouldUseImaggaDetectorWhenProviderIsImagga() {
        ObjectDetectionService service = configuration.objectDetectionService("imagga", "key", "secret", 70, -1, 1, 2, "", "clarifai", "main", "general-image-recognition", 0.70, 1, 5, new ObjectMapper());

        assertInstanceOf(ImaggaObjectDetectionService.class, service);
    }

    @Test
    void shouldUseClarifaiDetectorWhenProviderIsClarifai() {
        ObjectDetectionService service = configuration.objectDetectionService("clarifai", "", "", 70, -1, 1, 2, "pat", "clarifai", "main", "general-image-recognition", 0.70, 1, 5, new ObjectMapper());

        assertInstanceOf(ClarifaiObjectDetectionService.class, service);
    }

    @Test
    void shouldRejectImaggaProviderWhenCredentialsAreMissing() {
        assertThrows(
                IllegalStateException.class,
                () -> configuration.objectDetectionService("imagga", "", "", 70, -1, 1, 2, "", "clarifai", "main", "general-image-recognition", 0.70, 1, 5, new ObjectMapper())
        );
    }

    @Test
    void shouldRejectClarifaiProviderWhenPatIsMissing() {
        assertThrows(
                IllegalStateException.class,
                () -> configuration.objectDetectionService("clarifai", "", "", 70, -1, 1, 2, "", "clarifai", "main", "general-image-recognition", 0.70, 1, 5, new ObjectMapper())
        );
    }

    @Test
    void shouldRejectUnsupportedProvider() {
        assertThrows(
                IllegalArgumentException.class,
                () -> configuration.objectDetectionService("unknown", "", "", 70, -1, 1, 2, "", "clarifai", "main", "general-image-recognition", 0.70, 1, 5, new ObjectMapper())
        );
    }
}