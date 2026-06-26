package com.ss.imagedetection.service;

import com.ss.imagedetection.detector.ObjectDetectionService;
import com.ss.imagedetection.dto.CreateImageRequest;
import com.ss.imagedetection.dto.ImageResponse;
import com.ss.imagedetection.dto.PagedImageResponse;
import com.ss.imagedetection.entity.ImageEntity;
import com.ss.imagedetection.exception.ImageNotFoundException;
import com.ss.imagedetection.repository.ImageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageServiceTest {

    private final ImageRepository imageRepository = mock(ImageRepository.class);
    private final ObjectDetectionService objectDetectionService = mock(ObjectDetectionService.class);
    private final ImageService imageService = new ImageService(imageRepository, objectDetectionService);
    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void shouldTrimProvidedLabelAndSkipDetectionWhenDisabled() {
        CreateImageRequest request = new CreateImageRequest();
        request.setImageUrl("https://example.com/cat-photo.jpg");
        request.setLabel("  Custom label  ");
        request.setEnableDetection(false);
        when(imageRepository.save(any(ImageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImageResponse response = imageService.createImage(request);

        assertEquals("Custom label", response.label());
        assertTrue(response.detectedObjects().isEmpty());
        verify(objectDetectionService, never()).detectObjects(any());
    }

    @Test
    void shouldGenerateLabelAndStoreDetectedObjectsWhenDetectionEnabled() {
        CreateImageRequest request = new CreateImageRequest();
        request.setImageUrl("https://example.com/dog_tree-photo.jpg");
        request.setEnableDetection(true);
        when(objectDetectionService.detectObjects(request.getImageUrl())).thenReturn(List.of("Dog", "Tree"));
        when(imageRepository.save(any(ImageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImageResponse response = imageService.createImage(request);

        assertEquals("dog tree photo", response.label());
        assertEquals(List.of("dog", "tree"), response.detectedObjects());
    }

    @Test
    void shouldRejectObjectsQueryWithNoUsableValues() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> imageService.getImages(" , , ", pageable)
        );

        assertEquals("objects query parameter must contain at least one object name", exception.getMessage());
    }

    @Test
    void shouldNormalizeObjectSearchTermsBeforeRepositoryLookup() {
        when(imageRepository.findImageIdsByDetectedObjectNames(List.of("dog", "cat"), pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PagedImageResponse response = imageService.getImages(" Dog, cat, DOG ", pageable);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        verify(imageRepository).findImageIdsByDetectedObjectNames(List.of("dog", "cat"), pageable);
        verify(imageRepository, never()).findAllByIdIn(any());
    }

    @Test
    void shouldThrowImageNotFoundExceptionForUnknownId() {
        when(imageRepository.findWithDetectedObjectsById(99L)).thenReturn(Optional.empty());

        ImageNotFoundException exception = assertThrows(
                ImageNotFoundException.class,
                () -> imageService.getImageById(99L)
        );

        assertEquals("Image not found for id: 99", exception.getMessage());
    }
}