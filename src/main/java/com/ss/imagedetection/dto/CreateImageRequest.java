package com.ss.imagedetection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;

/**
 * Request body for image ingestion. The image URL must be a structurally valid HTTP(S) URL;
 * the optional label is resolved by the service when omitted or blank.
 */
public class CreateImageRequest {

    @Schema(description = "Public http/https URL of the image to ingest", example = "https://example.com/dog-park.jpg")
    @NotBlank(message = "imageUrl is required")
    // @URL validates the overall URL shape; @Pattern narrows accepted schemes to HTTP(S).
    @URL(message = "imageUrl must be a valid URL")
    @Pattern(regexp = "^https?://.*", message = "imageUrl must use http or https")
    private String imageUrl;

    @Schema(description = "Optional human-readable label for the image. If omitted, one is derived from the URL.", example = "Park photo")
    private String label;

    @Schema(description = "Whether object detection should be executed for the image", example = "true")
    private boolean enableDetection;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isEnableDetection() {
        return enableDetection;
    }

    public void setEnableDetection(boolean enableDetection) {
        this.enableDetection = enableDetection;
    }
}