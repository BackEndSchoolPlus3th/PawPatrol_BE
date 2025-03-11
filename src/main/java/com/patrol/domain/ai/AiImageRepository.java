package com.patrol.domain.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiImageRepository extends JpaRepository<AiImage, Long> {

    @Query("""
    SELECT i FROM AiImage i
    JOIN i.lostFoundPost p
    WHERE (i.embedding IS NOT NULL AND i.features IS NOT NULL) AND
        (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude))
        * cos(radians(p.longitude) - radians(:lng))
        + sin(radians(:lat)) * sin(radians(p.latitude)))) <= :radius
""")
    List<AiImage> findNearbyAiImages (
            @Param("lat") double latitude,
            @Param("lng") double longitude,
            @Param("radius") double radius);

    boolean existsByEmbeddingIsNotNullOrFeaturesIsNotNullAndPath (String path);
}
