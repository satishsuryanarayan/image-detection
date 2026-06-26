package com.heb.imagedetection.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Stored image metadata and detected objects")
public record ImageResponse(
        @Schema(description = "Database identifier of the image", example = "1")
        Long id,
        @Schema(description = "Original image URL", example = "https://example.com/dog-park.jpg")
        String imageUrl,
        @Schema(description = "Resolved label for the image", example = "Park photo")
        String label,
        @Schema(description = "Whether object detection was enabled for this image", example = "true")
        boolean detectionEnabled,
        @Schema(description = "Timestamp when the image was created")
        OffsetDateTime createdAt,
        @Schema(description = "Detected object names for the image")
        List<String> detectedObjects
) {
}