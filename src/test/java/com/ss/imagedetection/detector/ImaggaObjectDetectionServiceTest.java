package com.ss.imagedetection.detector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ss.imagedetection.exception.UpstreamServiceException;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

class ImaggaObjectDetectionServiceTest {

    @Test
    void shouldMapBadRequestFromImaggaToBadRequest() {
        HttpClient httpClient = new StubHttpClient(400);

        ImaggaObjectDetectionService service = new ImaggaObjectDetectionService(
                "api-key",
                "api-secret",
                70,
                -1,
                Duration.ofSeconds(2),
                new ObjectMapper(),
                httpClient
        );

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> service.detectObjects("https://example.com/image.jpg")
        );

        assertEquals(BAD_REQUEST, exception.getStatus());
        assertEquals("Imagga rejected request with status 400", exception.getMessage());
    }

    @Test
    void shouldMapAnyClientErrorFromImaggaToBadRequest() {
        HttpClient httpClient = new StubHttpClient(401);

        ImaggaObjectDetectionService service = new ImaggaObjectDetectionService(
                "api-key",
                "api-secret",
                70,
                -1,
                Duration.ofSeconds(2),
                new ObjectMapper(),
                httpClient
        );

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> service.detectObjects("https://example.com/image.jpg")
        );

        assertEquals(BAD_REQUEST, exception.getStatus());
        assertEquals("Imagga rejected request with status 401", exception.getMessage());
    }

    @Test
    void shouldMapServerErrorFromImaggaToBadGateway() {
        HttpClient httpClient = new StubHttpClient(500);

        ImaggaObjectDetectionService service = new ImaggaObjectDetectionService(
                "api-key",
                "api-secret",
                70,
                -1,
                Duration.ofSeconds(2),
                new ObjectMapper(),
                httpClient
        );

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> service.detectObjects("https://example.com/image.jpg")
        );

        assertEquals(BAD_GATEWAY, exception.getStatus());
        assertEquals("Imagga request failed with status 500", exception.getMessage());
    }

    @Test
    void shouldMapMalformedImaggaResponseToBadGateway() {
        HttpClient httpClient = new StubHttpClient(200, "not-json");

        ImaggaObjectDetectionService service = new ImaggaObjectDetectionService(
                "api-key",
                "api-secret",
                70,
                -1,
                Duration.ofSeconds(2),
                new ObjectMapper(),
                httpClient
        );

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> service.detectObjects("https://example.com/image.jpg")
        );

        assertEquals(BAD_GATEWAY, exception.getStatus());
        assertEquals("Imagga returned malformed response", exception.getMessage());
    }

    @Test
    void shouldMapImaggaTimeoutToGatewayTimeout() {
        HttpClient httpClient = StubHttpClient.throwing(new HttpTimeoutException("timed out"));

        ImaggaObjectDetectionService service = new ImaggaObjectDetectionService(
                "api-key",
                "api-secret",
                70,
                -1,
                Duration.ofSeconds(2),
                new ObjectMapper(),
                httpClient
        );

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> service.detectObjects("https://example.com/image.jpg")
        );

        assertEquals(GATEWAY_TIMEOUT, exception.getStatus());
        assertEquals("Imagga request timed out", exception.getMessage());
    }

    @Test
    void shouldMapImaggaConnectionTimeoutToServiceUnavailable() {
        HttpClient httpClient = StubHttpClient.throwing(new HttpConnectTimeoutException("connect timed out"));

        ImaggaObjectDetectionService service = new ImaggaObjectDetectionService(
                "api-key",
                "api-secret",
                70,
                -1,
                Duration.ofSeconds(2),
                new ObjectMapper(),
                httpClient
        );

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> service.detectObjects("https://example.com/image.jpg")
        );

        assertEquals(SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("Imagga service is unavailable", exception.getMessage());
    }

    @Test
    void shouldMapUnresponsiveImaggaToServiceUnavailable() {
        HttpClient httpClient = StubHttpClient.throwing(new IOException("connection refused"));

        ImaggaObjectDetectionService service = new ImaggaObjectDetectionService(
                "api-key",
                "api-secret",
                70,
                -1,
                Duration.ofSeconds(2),
                new ObjectMapper(),
                httpClient
        );

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> service.detectObjects("https://example.com/image.jpg")
        );

        assertEquals(SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("Imagga service is unavailable", exception.getMessage());
    }

    @Test
    void shouldUseConfiguredThresholdAndLimitInImaggaRequest() {
        StubHttpClient httpClient = new StubHttpClient(200);
        ImaggaObjectDetectionService service = new ImaggaObjectDetectionService(
                "api-key",
                "api-secret",
                85,
                10,
                Duration.ofSeconds(2),
                new ObjectMapper(),
                httpClient
        );

        service.detectObjects("https://example.com/image.jpg");

        String query = httpClient.lastRequest().uri().getQuery();
        assertTrue(query.contains("threshold=85"));
        assertTrue(query.contains("limit=10"));
    }

    @Test
    void shouldUseConfiguredRequestTimeoutInImaggaRequest() {
        StubHttpClient httpClient = new StubHttpClient(200);
        ImaggaObjectDetectionService service = new ImaggaObjectDetectionService(
                "api-key",
                "api-secret",
                85,
                10,
                Duration.ofSeconds(5),
                new ObjectMapper(),
                httpClient
        );

        service.detectObjects("https://example.com/image.jpg");

        assertFalse(httpClient.lastRequest().timeout().isEmpty());
        assertEquals(Duration.ofSeconds(5), httpClient.lastRequest().timeout().get());
    }

    private static class StubHttpClient extends HttpClient {

        private final int statusCode;
        private final String body;
        private final IOException exception;
        private HttpRequest lastRequest;

        private StubHttpClient(int statusCode) {
            this(statusCode, "{}");
        }

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
            if (exception != null) {
                throw exception;
            }
            return new StubHttpResponse<>(statusCode, request, (T) body);
        }

        private HttpRequest lastRequest() {
            return lastRequest;
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