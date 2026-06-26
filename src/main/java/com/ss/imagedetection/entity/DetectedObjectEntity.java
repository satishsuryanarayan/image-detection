package com.ss.imagedetection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Locale;

/**
 * Child entity representing one detected object associated with a specific image.
 * The same object name can appear in multiple rows when it is detected in multiple images.
 * Object names are normalized to lowercase. The composite index on (object_name, image_id)
 * supports object-name searches, while the image_id index supports loading detections by parent image.
 */
@Entity
@Table(
        name = "detected_objects",
        indexes = {
                @Index(name = "idx_detected_objects_object_name_image_id", columnList = "object_name, image_id"),
                @Index(name = "idx_detected_objects_image_id", columnList = "image_id")
        }
)
public class DetectedObjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stored as lowercase so lookups can stay index-friendly without LOWER(...) in the query.
    @Column(name = "object_name", nullable = false)
    private String objectName;

    // Many detected-object rows can belong to the same parent image.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private ImageEntity image;

    public DetectedObjectEntity() {
    }

    public DetectedObjectEntity(String objectName) {
        setObjectName(objectName);
    }

    public Long getId() {
        return id;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName == null ? null : objectName.toLowerCase(Locale.ROOT);
    }

    public ImageEntity getImage() {
        return image;
    }

    public void setImage(ImageEntity image) {
        this.image = image;
    }
}