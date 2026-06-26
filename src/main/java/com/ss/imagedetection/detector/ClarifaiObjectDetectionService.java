package com.ss.imagedetection.detector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ss.imagedetection.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Object-detection implementation backed by Clarifai's model outputs API.
 */
public class ClarifaiObjectDetectionService implements ObjectDetectionService {

    private static final Logger log = LoggerFactory.getLogger(ClarifaiObjectDetectionService.class);

    private static final String CLARIFAI_MODEL_OUTPUTS_ENDPOINT = "https://api.clarifai.com/v2/users/%s/apps/%s/models/%s/outputs";

    private final String pat;
    private final String userId;
    private final String appId;
    private final String modelId;
    private final double minConfidence;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public ClarifaiObjectDetectionService(
            String pat,
            String userId,
            String appId,
            String modelId,
            double minConfidence,
            long connectionTimeoutSeconds,
            long requestTimeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this(
                pat,
                userId,
                appId,
                modelId,
                minConfidence,
                Duration.ofSeconds(requestTimeoutSeconds),
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(connectionTimeoutSeconds))
                        .build()
        );
    }

    ClarifaiObjectDetectionService(
            String pat,
            String userId,
            String appId,
            String modelId,
            double minConfidence,
            Duration requestTimeout,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.pat = pat;
        this.userId = userId;
        this.appId = appId;
        this.modelId = modelId;
        this.minConfidence = minConfidence;
        this.requestTimeout = requestTimeout;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public List<String> detectObjects(String imageUrl) {
        log.info("Calling Clarifai for image url='{}'", imageUrl);
        HttpRequest request = HttpRequest.newBuilder(buildEndpointUri())
                .header(HttpHeaders.AUTHORIZATION, "Key " + pat)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(imageUrl)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400 && response.statusCode() < 500) {
                log.warn("Clarifai rejected request with status={} for url='{}'", response.statusCode(), imageUrl);
                throw new UpstreamServiceException(HttpStatus.BAD_REQUEST, "Clarifai rejected request with status " + response.statusCode());
            }

            if (response.statusCode() >= 500 && response.statusCode() < 600) {
                log.error("Clarifai request failed with status={} for url='{}'", response.statusCode(), imageUrl);
                throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Clarifai request failed with status " + response.statusCode());
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Clarifai request failed with unexpected status={} for url='{}'", response.statusCode(), imageUrl);
                throw new IllegalStateException("Clarifai request failed with status " + response.statusCode());
            }

            try {
                List<String> tags = extractConcepts(response.body());
                log.info("Clarifai returned {} detected objects for url='{}'", tags.size(), imageUrl);
                return tags;
            } catch (IOException exception) {
                log.error("Clarifai returned malformed response for url='{}'", imageUrl, exception);
                throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Clarifai returned malformed response", exception);
            }
        } catch (HttpConnectTimeoutException exception) {
            log.error("Failed to connect to Clarifai for url='{}'", imageUrl, exception);
            throw new UpstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Clarifai service is unavailable", exception);
        } catch (HttpTimeoutException exception) {
            log.error("Clarifai request timed out for url='{}'", imageUrl, exception);
            throw new UpstreamServiceException(HttpStatus.GATEWAY_TIMEOUT, "Clarifai request timed out", exception);
        } catch (IOException exception) {
            log.error("Failed to call Clarifai for url='{}'", imageUrl, exception);
            throw new UpstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Clarifai service is unavailable", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Clarifai call interrupted for url='{}'", imageUrl, exception);
            throw new IllegalStateException("Clarifai object detection call was interrupted", exception);
        }
    }

    private URI buildEndpointUri() {
        return URI.create(CLARIFAI_MODEL_OUTPUTS_ENDPOINT.formatted(userId, appId, modelId));
    }

    private String buildRequestBody(String imageUrl) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode input = objectMapper.createObjectNode();
        ObjectNode data = objectMapper.createObjectNode();
        ObjectNode image = objectMapper.createObjectNode();

        image.put("url", imageUrl);
        data.set("image", image);
        input.set("data", data);
        root.putArray("inputs").add(input);

        return root.toString();
    }

    private List<String> extractConcepts(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode concepts = root.path("outputs").path(0).path("data").path("concepts");
        Set<String> detectedObjects = new LinkedHashSet<>();

        for (JsonNode concept : concepts) {
            String name = concept.path("name").asText();
            double value = concept.path("value").asDouble(0.0);
            if (!name.isBlank() && value >= minConfidence) {
                detectedObjects.add(name.toLowerCase(Locale.ROOT));
            }
        }

        return List.copyOf(detectedObjects);
    }
}