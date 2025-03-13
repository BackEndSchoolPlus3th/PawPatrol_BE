package com.patrol.domain.lostFoundPost.repository;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LostFoundPostRepository extends JpaRepository<LostFoundPost, Long> {

    @Query(value = "SELECT f FROM LostFoundPost f " +
            "WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(f.latitude)) * " +
            "cos(radians(f.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
            "sin(radians(f.latitude)))) <= :radius/1000")
    List<LostFoundPost> findPostsWithinRadius(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radius") double radius
    );

    Page<LostFoundPost> findByStatus(PostStatus status, Pageable pageable);
    LostFoundPost findByPetId(Long petId);
    Page<LostFoundPost> findByAuthorId(Long authorId, Pageable pageable);

    Page<LostFoundPost> findByAuthorIdAndStatusIn(Long authorId, List<PostStatus> statuses, Pageable pageable);

    @Query("SELECT DISTINCT f.pet.id FROM LostFoundPost f WHERE f.pet IS NOT NULL")
    List<Long> findAllRegisteredPetIds();


    @Query(value = """
    SELECT * FROM lost_found_post lfp
    WHERE lfp.status = 'SIGHTED'
    AND lfp.animal_type = :animalType
    AND ST_Distance_Sphere(point(lfp.longitude, lfp.latitude), point(:longitude, :latitude)) <= :radius * 1000
""", nativeQuery = true)
    List<LostFoundPost> findSightedPostsWithinRadius(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radius") double radius,
            @Param("name") String name);

    @Query(value = """
    SELECT * FROM lost_found_post lfp
    WHERE lfp.status = 'FINDING'
    AND lfp.animal_type = :animalType
    AND ST_Distance_Sphere(point(lfp.longitude, lfp.latitude), point(:longitude, :latitude)) <= :radius * 1000
""", nativeQuery = true)
    List<LostFoundPost> findFindingPostsWithinRadius(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radius") double radius,
            @Param("name") String name);

    @Query("SELECT p FROM LostFoundPost p WHERE p.status = :status AND p.reward IS NOT NULL AND p.reward > 0 ORDER BY p.reward DESC")
    Page<LostFoundPost> findByStatusAndRewardNotNull(@Param("status") PostStatus status, Pageable pageable);
    Optional<LostFoundPost> findById (Long foundId);

}
