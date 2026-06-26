package com.ss.imagedetection.detector;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockObjectDetectionServiceTest {

    private final MockObjectDetectionService service = new MockObjectDetectionService(new Random(1L));

    @Test
    void shouldReturnRandomKnownObjects() {
        List<String> objects = service.detectObjects("https://example.com/person-dog-tree.jpg");

        assertFalse(objects.isEmpty());
        assertTrue(objects.size() <= 3);
        assertTrue(MockObjectDetectionService.KNOWN_OBJECTS.containsAll(objects));
    }

    @Test
    void shouldAvoidDuplicateDetections() {
        List<String> objects = service.detectObjects("https://example.com/dog-dog-dog.jpg");

        assertTrue(objects.size() <= new HashSet<>(objects).size());
    }

    @Test
    void shouldReturnKnownObjectsWhenUrlHasNoKnownKeywords() {
        List<String> objects = service.detectObjects("https://example.com/abstract-photo.jpg");

        assertFalse(objects.isEmpty());
        assertTrue(MockObjectDetectionService.KNOWN_OBJECTS.containsAll(objects));
        assertFalse(objects.contains("image"));
        assertFalse(objects.contains("unknown-object"));
    }
}