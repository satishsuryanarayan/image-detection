package com.heb.imagedetection.detector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockObjectDetectionServiceTest {

    private final MockObjectDetectionService service = new MockObjectDetectionService();

    @Test
    void shouldDetectKnownObjectsInDeterministicOrder() {
        List<String> objects = service.detectObjects("https://example.com/person-dog-tree.jpg");

        assertEquals(List.of("dog", "tree", "person"), objects);
    }

    @Test
    void shouldAvoidDuplicateDetections() {
        List<String> objects = service.detectObjects("https://example.com/dog-dog-dog.jpg");

        assertEquals(List.of("dog"), objects);
    }

    @Test
    void shouldReturnFallbackObjectsWhenUrlHasNoKnownKeywords() {
        List<String> objects = service.detectObjects("https://example.com/abstract-photo.jpg");

        assertEquals(List.of("image", "unknown-object"), objects);
    }
}