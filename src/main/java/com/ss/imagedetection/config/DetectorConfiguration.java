package com.ss.imagedetection.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Chooses the detector implementation based on whether Imagga credentials are configured.
 */
@Configuration
public class DetectorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DetectorConfiguration.class);

    @Bean
    public ObjectDetectionService objectDetectionService(
            @Value("${imagga.api-key:}") String apiKey,
            @Value("${imagga.api-secret:}") String apiSecret,
            @Value("${imagga.threshold:70}") int threshold,
            @Value("${imagga.limit:-1}") int limit,
            @Value("${imagga.connection-timeout-seconds:1}") long connectionTimeoutSeconds,
            @Value("${imagga.request-timeout-seconds:2}") long requestTimeoutSeconds,
            ObjectMapper objectMapper
    ) {
        if (StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret)) {
            log.info("Using ImaggaObjectDetectionService because credentials are configured");
            return new ImaggaObjectDetectionService(apiKey, apiSecret, threshold, limit, connectionTimeoutSeconds, requestTimeoutSeconds, objectMapper);
        }

        log.info("Using MockObjectDetectionService because Imagga credentials are not configured");
        return new MockObjectDetectionService();
    }
}