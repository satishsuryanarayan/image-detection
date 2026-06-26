package com.ss.imagedetection.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ss.imagedetection.detector.ImaggaObjectDetectionService;
import com.ss.imagedetection.detector.MockObjectDetectionService;
import com.ss.imagedetection.detector.ObjectDetectionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DetectorConfigurationTest {

    private final DetectorConfiguration configuration = new DetectorConfiguration();

    @Test
    void shouldUseMockDetectorWhenCredentialsAreMissing() {
        ObjectDetectionService service = configuration.objectDetectionService("", "", 70, -1, 1, 2, new ObjectMapper());

        assertInstanceOf(MockObjectDetectionService.class, service);
    }

    @Test
    void shouldUseImaggaDetectorWhenCredentialsAreConfigured() {
        ObjectDetectionService service = configuration.objectDetectionService("key", "secret", 70, -1, 1, 2, new ObjectMapper());

        assertInstanceOf(ImaggaObjectDetectionService.class, service);
    }
}