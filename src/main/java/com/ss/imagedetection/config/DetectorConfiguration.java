package com.ss.imagedetection.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ss.imagedetection.detector.ClarifaiObjectDetectionService;
import com.ss.imagedetection.detector.ImaggaObjectDetectionService;
import com.ss.imagedetection.detector.MockObjectDetectionService;
import com.ss.imagedetection.detector.ObjectDetectionService;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Chooses the detector implementation based on the configured detector provider.
 */
@Configuration
public class DetectorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DetectorConfiguration.class);

    @Bean
    public ObjectDetectionService objectDetectionService(
            @Value("${detector.provider:mock}") String provider,
            @Value("${imagga.api-key:}") String imaggaApiKey,
            @Value("${imagga.api-secret:}") String imaggaApiSecret,
            @Value("${imagga.threshold:70}") int imaggaThreshold,
            @Value("${imagga.limit:-1}") int imaggaLimit,
            @Value("${imagga.connection-timeout-seconds:1}") long imaggaConnectionTimeoutSeconds,
            @Value("${imagga.request-timeout-seconds:2}") long imaggaRequestTimeoutSeconds,
            @Value("${clarifai.pat:}") String clarifaiPat,
            @Value("${clarifai.user-id:clarifai}") String clarifaiUserId,
            @Value("${clarifai.app-id:main}") String clarifaiAppId,
            @Value("${clarifai.model-id:general-image-recognition}") String clarifaiModelId,
            @Value("${clarifai.min-confidence:0.70}") double clarifaiMinConfidence,
            @Value("${clarifai.connection-timeout-seconds:1}") long clarifaiConnectionTimeoutSeconds,
            @Value("${clarifai.request-timeout-seconds:5}") long clarifaiRequestTimeoutSeconds,
            ObjectMapper objectMapper
    ) {
        return switch (provider.trim().toLowerCase()) {
            case "mock" -> {
                log.info("Using MockObjectDetectionService because detector.provider=mock");
                yield new MockObjectDetectionService();
            }
            case "imagga" -> {
                requireText(imaggaApiKey, "imagga.api-key is required when detector.provider=imagga");
                requireText(imaggaApiSecret, "imagga.api-secret is required when detector.provider=imagga");
                log.info("Using ImaggaObjectDetectionService because detector.provider=imagga");
                yield new ImaggaObjectDetectionService(imaggaApiKey, imaggaApiSecret, imaggaThreshold, imaggaLimit, imaggaConnectionTimeoutSeconds, imaggaRequestTimeoutSeconds, objectMapper);
            }
            case "clarifai" -> {
                requireText(clarifaiPat, "clarifai.pat is required when detector.provider=clarifai");
                log.info("Using ClarifaiObjectDetectionService because detector.provider=clarifai");
                yield new ClarifaiObjectDetectionService(clarifaiPat, clarifaiUserId, clarifaiAppId, clarifaiModelId, clarifaiMinConfidence, clarifaiConnectionTimeoutSeconds, clarifaiRequestTimeoutSeconds, objectMapper);
            }
            default -> throw new IllegalArgumentException("Unsupported detector.provider: " + provider + ". Supported values are mock, imagga, clarifai");
        };
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
    }
}