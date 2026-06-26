package com.heb.imagedetection.detector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heb.imagedetection.exception.UpstreamServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Real object-detection implementation backed by Imagga's tagging API.
 * This implementation is instantiated only when Imagga credentials are configured.
 * Upstream Imagga failures are translated to API-facing statuses by throwing
 * {@link UpstreamServiceException}; unexpected local failures continue through
 * the generic server-error path.
 */
public class ImaggaObjectDetectionService implements ObjectDetectionService {

    private static final Logger log = LoggerFactory.getLogger(ImaggaObjectDetectionService.class);

    private static final String IMAGGA_TAGGING_ENDPOINT = "https://api.imagga.com/v2/tags";

    private final String apiKey;
    private final String apiSecret;
    private final int threshold;
    private final int limit;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public ImaggaObjectDetectionService(
            String apiKey,
            String apiSecret,
            int threshold,
            int limit,
            long connectionTimeoutSeconds,
            long requestTimeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.threshold = threshold;
        this.limit = limit;
        this.objectMapper = objectMapper;
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                // Connection timeout limits establishing the TCP/TLS connection.
                .connectTimeout(Duration.ofSeconds(connectionTimeoutSeconds))
                .build();
    }

    ImaggaObjectDetectionService(String apiKey, String apiSecret, int threshold, int limit, Duration requestTimeout, ObjectMapper objectMapper, HttpClient httpClient) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.threshold = threshold;
        this.limit = limit;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public List<String> detectObjects(String imageUrl) {
        log.info("Calling Imagga for image url='{}'", imageUrl);
        URI requestUri = UriComponentsBuilder.fromUriString(IMAGGA_TAGGING_ENDPOINT)
                .queryParam("image_url", imageUrl)
                .queryParam("threshold", threshold)
                .queryParam("limit", limit)
                .build()
                .toUri();

        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .header(HttpHeaders.AUTHORIZATION, buildBasicAuthHeader())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // Request timeout limits the full synchronous request/response wait.
                .timeout(requestTimeout)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400 && response.statusCode() < 500) {
                log.warn("Imagga rejected request with status={} for url='{}'", response.statusCode(), imageUrl);
                throw new UpstreamServiceException(HttpStatus.BAD_REQUEST, "Imagga rejected request with status " + response.statusCode());
            }

            if (response.statusCode() >= 500 && response.statusCode() < 600) {
                log.error("Imagga request failed with status={} for url='{}'", response.statusCode(), imageUrl);
                throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Imagga request failed with status " + response.statusCode());
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Imagga request failed with unexpected status={} for url='{}'", response.statusCode(), imageUrl);
                throw new IllegalStateException("Imagga request failed with status " + response.statusCode());
            }

            List<String> tags;
            try {
                tags = extractTags(response.body());
            } catch (IOException exception) {
                log.error("Imagga returned malformed response for url='{}'", imageUrl, exception);
                throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Imagga returned malformed response", exception);
            }
            log.info("Imagga returned {} detected objects for url='{}'", tags.size(), imageUrl);
            return tags;
        } catch (HttpConnectTimeoutException exception) {
            log.error("Failed to connect to Imagga for url='{}'", imageUrl, exception);
            throw new UpstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Imagga service is unavailable", exception);
        } catch (HttpTimeoutException exception) {
            log.error("Imagga request timed out for url='{}'", imageUrl, exception);
            throw new UpstreamServiceException(HttpStatus.GATEWAY_TIMEOUT, "Imagga request timed out", exception);
        } catch (IOException exception) {
            log.error("Failed to call Imagga for url='{}'", imageUrl, exception);
            throw new UpstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Imagga service is unavailable", exception);
        } catch (InterruptedException exception) {
            // Preserve the interrupt signal for upstream framework/executor code.
            Thread.currentThread().interrupt();
            log.error("Imagga call interrupted for url='{}'", imageUrl, exception);
            throw new IllegalStateException("Imagga object detection call was interrupted", exception);
        }
    }

    private List<String> extractTags(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode tags = root.path("result").path("tags");
        Set<String> detectedObjects = new LinkedHashSet<>();

        for (JsonNode tag : tags) {
            String value = tag.path("tag").path("en").asText();
            if (!value.isBlank()) {
                detectedObjects.add(value.toLowerCase());
            }
        }

        return List.copyOf(detectedObjects);
    }

    private String buildBasicAuthHeader() {
        String credentials = apiKey + ":" + apiSecret;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}