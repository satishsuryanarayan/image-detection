package com.heb.imagedetection.detector;

import java.util.List;

public interface ObjectDetectionService {

    /**
     * Detects object names for a remotely hosted image.
     */
    List<String> detectObjects(String imageUrl);
}