CREATE DATABASE IF NOT EXISTS image_detection;

USE image_detection;

CREATE TABLE IF NOT EXISTS images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    image_url VARCHAR(2048) NOT NULL,
    label VARCHAR(255) NOT NULL,
    detection_enabled BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS detected_objects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    object_name VARCHAR(255) NOT NULL,
    image_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_detected_objects_object_name_image_id (object_name, image_id),
    INDEX idx_detected_objects_image_id (image_id),
    CONSTRAINT fk_detected_objects_image
        FOREIGN KEY (image_id)
        REFERENCES images (id)
        ON DELETE CASCADE
);