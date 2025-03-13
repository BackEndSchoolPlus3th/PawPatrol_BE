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

    // 현재 로그인된 멤버의 모든 게시글 페이징 처리 후 가져오기
    Page<LostFoundPost> findByAuthorId(Long authorId, Pageable pageable);

    // 게시글 상태에 따라 신고글/제보글 가져오기
    Page<LostFoundPost> findByAuthorIdAndStatusIn(Long authorId, List<PostStatus> statuses, Pageable pageable);

    // petId를 pet 엔티티의 id로 변경하여 조회하도록 쿼리 수정
    @Query("SELECT DISTINCT f.pet.id FROM LostFoundPost f WHERE f.pet IS NOT NULL")
    List<Long> findAllRegisteredPetIds();  // 등록된 petId 목록을 가져오는 메서드


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
