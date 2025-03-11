package com.patrol.domain.image.repository;

import com.patrol.domain.image.entity.Image;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findById(Long Id);
    List<Image> findAllByFoundId(Long postId);
    List<Image> findAllByAnimalId(Long animalId);

    void delete(Image image);

    Image findByAnimalId(Long petId);

    List<Image> findByPath(String path);

    Optional<Image> findFirstByFoundIdOrderByCreatedAtAsc(Long foundId);

    Image findByFoundId (Long foundId);
}
