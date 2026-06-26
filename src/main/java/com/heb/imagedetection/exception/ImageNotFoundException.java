package com.heb.imagedetection.exception;

/**
 * Raised when a requested image id does not exist in persistent storage.
 */
public class ImageNotFoundException extends RuntimeException {

    public ImageNotFoundException(Long imageId) {
        super("Image not found for id: " + imageId);
    }
}