package com.ss.imagedetection.detector;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic local detector used for demo and interview purposes.
 * It infers object names from keywords in the image URL so the application works
 * without external API credentials.
 */
public class MockObjectDetectionService implements ObjectDetectionService {

    private static final List<String> KNOWN_OBJECTS = List.of(
            "cat", "dog", "car", "tree", "person", "bike", "bird", "flower"
    );

    @Override
    public List<String> detectObjects(String imageUrl) {
        // Preserve insertion order and prevent duplicate detections for the same image.
        String normalized = imageUrl.toLowerCase(Locale.ROOT);
        Set<String> detected = new LinkedHashSet<>();

        for (String candidate : KNOWN_OBJECTS) {
            if (normalized.contains(candidate)) {
                detected.add(candidate);
            }
        }

        if (detected.isEmpty()) {
            detected.addAll(Arrays.asList("image", "unknown-object"));
        }

        return List.copyOf(detected);
    }
}