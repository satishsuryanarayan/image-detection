package com.heb.imagedetection.repository;

import com.heb.imagedetection.entity.ImageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * Repository for loading images and their detected objects.
 * The search-by-object flow intentionally uses two queries instead of a filtered fetch join:
 * 1) find the matching image ids
 * 2) load the matching images with their full detectedObjects collection
 * This avoids partially populated child collections when filtering on joined rows.
 * Detected object names are stored in lowercase, which allows the object-name filter
 * to use a normal indexed equality/in-list predicate instead of wrapping the column in LOWER(...).
 */
public interface ImageRepository extends JpaRepository<ImageEntity, Long> {

    // Return all images with their detected objects eagerly loaded for response mapping.
    @Override
    @NonNull
    @EntityGraph(attributePaths = "detectedObjects")
    @Query("select i from ImageEntity i order by i.createdAt desc")
    List<ImageEntity> findAll();

    // First step of paginated listing: page parent ids only, avoiding collection-fetch pagination issues.
    @Query(
            value = "select i.id from ImageEntity i",
            countQuery = "select count(i) from ImageEntity i"
    )
    Page<Long> findAllImageIds(Pageable pageable);

    // Used by GET /images/{imageId} so the full object list is available in one repository call.
    @EntityGraph(attributePaths = "detectedObjects")
    Optional<ImageEntity> findWithDetectedObjectsById(Long id);

    // First step of filtered search: determine which images match any requested lowercase object name.
    @Query(
            value = """
            select distinct i.id from ImageEntity i
            join i.detectedObjects d
            where d.objectName in :objects
            """,
            countQuery = """
            select count(distinct i.id) from ImageEntity i
            join i.detectedObjects d
            where d.objectName in :objects
            """
    )
    Page<Long> findImageIdsByDetectedObjectNames(@Param("objects") List<String> objects, Pageable pageable);

    // Second step of filtered search: load the full image graph for the matched ids.
    @EntityGraph(attributePaths = "detectedObjects")
    @Query("""
            select distinct i from ImageEntity i
            where i.id in :ids
            """)
    List<ImageEntity> findAllByIdIn(@Param("ids") List<Long> ids);
}