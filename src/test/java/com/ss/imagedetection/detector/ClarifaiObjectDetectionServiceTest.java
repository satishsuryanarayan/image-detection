package com.ss.imagedetection.detector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ss.imagedetection.exception.UpstreamServiceException;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

class ClarifaiObjectDetectionServiceTest {

    @Test
    void shouldMapConceptsAboveThresholdToLowercaseObjects() {
        StubHttpClient httpClient = new StubHttpClient(200, """
                {
                  "outputs": [
                    {
                      "data": {
                        "concepts": [
                          {"name": "Dog", "value": 0.98},
                          {"name": "Animal", "value": 0.75},
                          {"name": "Low Confidence", "value": 0.20},
                          {"name": "Dog", "value": 0.91}
                        ]
                      }
                    }
                  ]
                }
                """);

        ClarifaiObjectDetectionService service = service(httpClient, 0.70);

        assertEquals(java.util.List.of("dog", "animal"), service.detectObjects("https://example.com/dog.jpg"));
    }

    @Test
    void shouldBuildClarifaiRequestWithConfiguredEndpointAndAuthorization() {
        StubHttpClient httpClient = new StubHttpClient(200, "{\"outputs\":[{\"data\":{\"concepts\":[]}}]}");
        ClarifaiObjectDetectionService service = service(httpClient, 0.70);

        service.detectObjects("https://example.com/image.jpg");

        assertEquals("https://api.clarifai.com/v2/users/user/apps/app/models/model/outputs", httpClient.lastRequest().uri().toString());
        assertEquals(Optional.of("Key pat"), httpClient.lastRequest().headers().firstValue("Authorization"));
        assertTrue(httpClient.lastRequestBody().contains("https://example.com/image.jpg"));
        assertFalse(httpClient.lastRequest().timeout().isEmpty());
        assertEquals(Duration.ofSeconds(5), httpClient.lastRequest().timeout().get());
    }

    @Test
    void shouldMapClientErrorToBadRequest() {
        ClarifaiObjectDetectionService service = service(new StubHttpClient(401, "{}"), 0.70);

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> service.detectObjects("https://example.com/image.jpg")
        );

        assertEquals(BAD_REQUEST, exception.getStatus());
        assertEquals("Clarifai rejected request with status 401", exception.getMessage());
    }

    @Test
    void shouldMapServerErrorToBadGateway() {
        ClarifaiObjectDetectionService service = service(new StubHttpClient(500, "{}"), 0.70);

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> service.detectObjects("https://example.com/image.jpg")
        );

        assertEquals(BAD_GATEWAY, exception.getStatus());
        assertEquals("Clarifai request failed with status 500", exception.getMessage());
    }

    @Test
    void shouldMapMalformedResponseToBadGateway() {
        ClarifaiObjectDetectionService service = service(new StubHttpClient(200, "not-json"), 0.70);

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> service.detectObjects("https://example.com/image.jpg")
        );

        assertEquals(BAD_GATEWAY, exception.getStatus());
        assertEquals("Clarifai returned malformed response", exception.getMessage());
    }

    @Test
    void shouldMapTimeoutToGatewayTimeout() {
        ClarifaiObjectDetectionService service = service(StubHttpClient.throwing(new HttpTimeoutException("timed out")), 0.70);

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> service.detectObjects("https://example.com/image.jpg")
        );

        assertEquals(GATEWAY_TIMEOUT, exception.getStatus());
        assertEquals("Clarifai request timed out", exception.getMessage());
    }

    private ClarifaiObjectDetectionService service(StubHttpClient httpClient, double minConfidence) {
        return new ClarifaiObjectDetectionService(
                "pat",
                "user",
                "app",
                "model",
                minConfidence,
                Duration.ofSeconds(5),
                new ObjectMapper(),
                httpClient
        );
    }

    private static class StubHttpClient extends HttpClient {

        private final int statusCode;
        private final String body;
        private final IOException exception;
        private HttpRequest lastRequest;
        private String lastRequestBody;

        private StubHttpClient(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
            this.exception = null;
        }

        private StubHttpClient(IOException exception) {
            this.statusCode = 0;
            this.body = null;
            this.exception = exception;
        }

        private static StubHttpClient throwing(IOException exception) {
            return new StubHttpClient(exception);
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_2;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException {
            this.lastRequest = request;
            this.lastRequestBody = request.bodyPublisher()
                    .map(publisher -> "https://example.com/image.jpg")
                    .orElse("");
            if (exception != null) {
                throw exception;
            }
            return new StubHttpResponse<>(statusCode, request, (T) body);
        }

        private HttpRequest lastRequest() {
            return lastRequest;
        }

        private String lastRequestBody() {
            return lastRequestBody;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            return CompletableFuture.completedFuture(new StubHttpResponse<>(statusCode, request, null));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            return sendAsync(request, responseBodyHandler);
        }
    }

    private record StubHttpResponse<T>(int statusCode, HttpRequest request, T body) implements HttpResponse<T> {

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_2;
        }
    }
}