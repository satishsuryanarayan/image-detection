package com.ss.imagedetection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Paginated image search/list response")
public record PagedImageResponse(
        @Schema(description = "Images on the current page")
        List<ImageResponse> content,
        @Schema(description = "Zero-based page number", example = "0")
        int page,
        @Schema(description = "Requested page size", example = "20")
        int size,
        @Schema(description = "Total number of matching images", example = "42")
        long totalElements,
        @Schema(description = "Total number of available pages", example = "3")
        int totalPages,
        @Schema(description = "Whether this is the first page", example = "true")
        boolean first,
        @Schema(description = "Whether this is the last page", example = "false")
        boolean last
) {

    public static PagedImageResponse from(Page<ImageResponse> page) {
        return new PagedImageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}