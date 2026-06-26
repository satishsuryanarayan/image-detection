package com.ss.imagedetection.detector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Local detector used for demo and interview purposes.
 * It randomly picks known object names so the application works without external API credentials.
 */
public class MockObjectDetectionService implements ObjectDetectionService {

    static final List<String> KNOWN_OBJECTS = List.of(
            "cat", "dog", "car", "tree", "person", "bike", "bird", "flower",
            "apple", "banana", "chair", "table", "phone", "laptop", "book", "cup",
            "bottle", "bus", "truck", "house", "road", "mountain", "beach", "boat"
    );

    private static final int MIN_DETECTED_OBJECTS = 1;
    private static final int MAX_DETECTED_OBJECTS = 3;

    private final Random random;

    public MockObjectDetectionService() {
        this(new Random());
    }

    MockObjectDetectionService(Random random) {
        this.random = random;
    }

    @Override
    public List<String> detectObjects(String imageUrl) {
        List<String> candidates = new ArrayList<>(KNOWN_OBJECTS);
        Collections.shuffle(candidates, random);
        int detectedObjectCount = random.nextInt(MAX_DETECTED_OBJECTS - MIN_DETECTED_OBJECTS + 1) + MIN_DETECTED_OBJECTS;
        return List.copyOf(candidates.subList(0, detectedObjectCount));
    }
}