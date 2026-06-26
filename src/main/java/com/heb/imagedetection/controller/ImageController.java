package com.heb.imagedetection.controller;

import com.heb.imagedetection.dto.ApiErrorResponse;
import com.heb.imagedetection.dto.CreateImageRequest;
import com.heb.imagedetection.dto.ImageResponse;
import com.heb.imagedetection.dto.PagedImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.heb.imagedetection.service.ImageService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for creating image records, listing/searching stored images, and loading one image by id.
 * Business rules and transaction boundaries remain in {@link ImageService}; this class handles HTTP mapping.
 */
@RestController
@RequestMapping("/images")
@Tag(name = "Images", description = "Operations for image ingestion and object-based search")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping
    @Operation(summary = "Create an image record", description = "Stores an image URL, generates a label if needed, and optionally runs object detection.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image created successfully", content = @Content(schema = @Schema(implementation = ImageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or Imagga client error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Imagga returned a server error or malformed response", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Imagga service is unavailable", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "504", description = "Imagga request timed out", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ImageResponse> createImage(@Valid @RequestBody CreateImageRequest request) {
        log.info("Received request to create image for url='{}' detectionEnabled={}", request.getImageUrl(), request.isEnableDetection());
        return ResponseEntity.ok(imageService.createImage(request));
    }

    @GetMapping
    @Operation(summary = "List or search images", description = "Returns a paginated list of stored images when the query is missing or blank, or filters images by any requested detected object name when the objects query parameter is provided.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Images returned successfully", content = @Content(schema = @Schema(implementation = PagedImageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid objects query parameter", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PagedImageResponse> getImages(
            @RequestParam(required = false) String objects,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("Received request to list/search images with objects='{}' page={} size={}", objects, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(imageService.getImages(objects, pageable));
    }

    @GetMapping("/{imageId}")
    @Operation(summary = "Get image by id", description = "Returns metadata and detected objects for a single stored image.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image found", content = @Content(schema = @Schema(implementation = ImageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Image not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ImageResponse> getImageById(@PathVariable Long imageId) {
        log.info("Received request to fetch image id={}", imageId);
        return ResponseEntity.ok(imageService.getImageById(imageId));
    }
}