package com.ss.imagedetection.service;

import com.ss.imagedetection.detector.ObjectDetectionService;
import com.ss.imagedetection.dto.CreateImageRequest;
import com.ss.imagedetection.dto.ImageResponse;
import com.ss.imagedetection.dto.PagedImageResponse;
import com.ss.imagedetection.entity.DetectedObjectEntity;
import com.ss.imagedetection.entity.ImageEntity;
import com.ss.imagedetection.exception.ImageNotFoundException;
import com.ss.imagedetection.repository.ImageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public PagedImageResponse getImages(String objects, Pageable pageable) {
        if (!StringUtils.hasText(objects)) {
            log.info("Listing stored images because no object filter was provided: page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
            return toPagedResponse(imageRepository.findAllImageIds(pageable), pageable);
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

        log.info("Searching images by detected objects {}: page={} size={}", normalizedObjects, pageable.getPageNumber(), pageable.getPageSize());

        // Two-step search avoids the ambiguity of using a filtered fetch join on a collection.
        Page<Long> imageIdPage = imageRepository.findImageIdsByDetectedObjectNames(normalizedObjects, pageable);
        if (imageIdPage.isEmpty()) {
            log.info("No images matched detected objects {}", normalizedObjects);
            return PagedImageResponse.from(Page.empty(pageable));
        }

        PagedImageResponse response = toPagedResponse(imageIdPage, pageable);
        log.info("Returning {} matched images for objects {}", response.content().size(), normalizedObjects);
        return response;
    }

    private PagedImageResponse toPagedResponse(Page<Long> imageIdPage, Pageable pageable) {
        if (imageIdPage.isEmpty()) {
            return PagedImageResponse.from(Page.empty(pageable));
        }

        List<Long> imageIds = imageIdPage.getContent();
        Map<Long, ImageEntity> imagesById = imageRepository.findAllByIdIn(imageIds)
                .stream()
                .collect(Collectors.toMap(ImageEntity::getId, Function.identity()));
        List<ImageResponse> content = imageIds.stream()
                .map(imagesById::get)
                .filter(image -> image != null)
                .map(this::toResponse)
                .toList();

        return PagedImageResponse.from(new PageImpl<>(content, pageable, imageIdPage.getTotalElements()));
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