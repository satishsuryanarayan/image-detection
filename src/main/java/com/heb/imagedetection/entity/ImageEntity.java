package com.heb.imagedetection.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent entity representing an ingested image and its metadata.
 */
@Entity
@Table(name = "images")
public class ImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(nullable = false)
    private String label;

    @Column(name = "detection_enabled", nullable = false)
    private boolean detectionEnabled;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // Child detections are persisted and removed with the parent image lifecycle.
    @OneToMany(mappedBy = "image", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetectedObjectEntity> detectedObjects = new ArrayList<>();

    public Long getId() {
        return id;
    }

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

    public boolean isDetectionEnabled() {
        return detectionEnabled;
    }

    public void setDetectionEnabled(boolean detectionEnabled) {
        this.detectionEnabled = detectionEnabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<DetectedObjectEntity> getDetectedObjects() {
        return detectedObjects;
    }

    // Helper keeps both sides of the bidirectional association in sync.
    public void addDetectedObject(DetectedObjectEntity detectedObject) {
        detectedObjects.add(detectedObject);
        detectedObject.setImage(this);
    }
}