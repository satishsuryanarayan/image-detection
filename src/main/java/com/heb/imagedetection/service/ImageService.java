package com.heb.imagedetection.service;

import com.heb.imagedetection.detector.ObjectDetectionService;
import com.heb.imagedetection.dto.CreateImageRequest;
import com.heb.imagedetection.dto.ImageResponse;
import com.heb.imagedetection.entity.DetectedObjectEntity;
import com.heb.imagedetection.entity.ImageEntity;
import com.heb.imagedetection.exception.ImageNotFoundException;
import com.heb.imagedetection.repository.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Coordinates image creation, search, and response shaping.
 * Transactions are defined at the service layer, so each use case has a single persistence boundary.
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private final ImageRepository imageRepository;
    private final ObjectDetectionService objectDetectionService;

    public ImageService(ImageRepository imageRepository, ObjectDetectionService objectDetectionService) {
        this.imageRepository = imageRepository;
        this.objectDetectionService = objectDetectionService;
    }

    /**
     * Creates an image record and, when requested, persists the detected objects in the same transaction.
     */
    @Transactional(rollbackFor = Exception.class)
    public ImageResponse createImage(CreateImageRequest request) {
        log.debug("Creating image entity for url='{}'", request.getImageUrl());
        ImageEntity image = new ImageEntity();
        image.setImageUrl(request.getImageUrl());
        image.setLabel(resolveLabel(request));
        image.setDetectionEnabled(request.isEnableDetection());
        image.setCreatedAt(OffsetDateTime.now());

        if (request.isEnableDetection()) {
            log.info("Running object detection for url='{}'", request.getImageUrl());
            objectDetectionService.detectObjects(request.getImageUrl())
                    .forEach(objectName -> image.addDetectedObject(new DetectedObjectEntity(objectName)));
        }

        ImageResponse saved = toResponse(imageRepository.save(image));
        log.info("Stored image id={} label='{}' detectedObjectCount={}", saved.id(), saved.label(), saved.detectedObjects().size());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getImages(String objects) {
        if (!StringUtils.hasText(objects)) {
            log.info("Listing all stored images because no object filter was provided");
            return imageRepository.findAll().stream().map(this::toResponse).toList();
        }

        // Normalize user input so matching stays case-insensitive while preserving an index-friendly query.
        List<String> normalizedObjects = Arrays.stream(objects.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .distinct()
                .toList();
        if (normalizedObjects.isEmpty()) {
            log.warn("Rejected objects query because it contained no usable values: raw='{}'", objects);
            throw new IllegalArgumentException("objects query parameter must contain at least one object name");
        }

        log.info("Searching images by detected objects {}", normalizedObjects);

        // Two-step search avoids the ambiguity of using a filtered fetch join on a collection.
        List<Long> imageIds = imageRepository.findImageIdsByDetectedObjectNames(normalizedObjects);
        if (imageIds.isEmpty()) {
            log.info("No images matched detected objects {}", normalizedObjects);
            return List.of();
        }

        List<ImageResponse> responses = imageRepository.findAllByIdInOrderByCreatedAtDesc(imageIds).stream().map(this::toResponse).toList();
        log.info("Returning {} matched images for objects {}", responses.size(), normalizedObjects);
        return responses;
    }

    @Transactional(readOnly = true)
    public ImageResponse getImageById(Long imageId) {
        log.debug("Loading image id={}", imageId);
        ImageEntity image = imageRepository.findWithDetectedObjectsById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(imageId));
        return toResponse(image);
    }

    private String resolveLabel(CreateImageRequest request) {
        if (StringUtils.hasText(request.getLabel())) {
            return request.getLabel().trim();
        }

        // Derive a readable fallback label from the trailing portion of the image URL path.
        String path = URI.create(request.getImageUrl()).getPath();
        String candidate = (path == null || path.isBlank()) ? "image" : path.substring(path.lastIndexOf('/') + 1);
        candidate = candidate.isBlank() ? "image" : candidate;
        int extensionIndex = candidate.lastIndexOf('.');
        if (extensionIndex > 0) {
            candidate = candidate.substring(0, extensionIndex);
        }
        return candidate.replaceAll("[-_]+", " ").trim();
    }

    private ImageResponse toResponse(ImageEntity image) {
        return new ImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getLabel(),
                image.isDetectionEnabled(),
                image.getCreatedAt(),
                image.getDetectedObjects().stream().map(DetectedObjectEntity::getObjectName).toList()
        );
    }
}