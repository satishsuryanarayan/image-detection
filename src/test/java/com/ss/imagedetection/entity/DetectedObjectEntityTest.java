package com.ss.imagedetection.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DetectedObjectEntityTest {

    @Test
    void shouldNormalizeObjectNameToLowercaseInConstructor() {
        DetectedObjectEntity detectedObject = new DetectedObjectEntity("DoG");

        assertEquals("dog", detectedObject.getObjectName());
    }

    @Test
    void shouldNormalizeObjectNameToLowercaseInSetter() {
        DetectedObjectEntity detectedObject = new DetectedObjectEntity();

        detectedObject.setObjectName("CaT");

        assertEquals("cat", detectedObject.getObjectName());
    }

    @Test
    void shouldAllowNullObjectNameForJpaConstruction() {
        DetectedObjectEntity detectedObject = new DetectedObjectEntity();

        detectedObject.setObjectName(null);

        assertNull(detectedObject.getObjectName());
    }
}