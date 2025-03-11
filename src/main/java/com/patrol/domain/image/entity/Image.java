package com.patrol.domain.image.entity;

import com.patrol.domain.animal.enums.AnimalType;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String path;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "found_id", nullable = true)
    private Long foundId;

    @Column(name= "animal_id", nullable= true)
    private Long animalId;

    @Enumerated(EnumType.STRING)
    private PostStatus status;

    @Enumerated(EnumType.STRING)
    private AnimalType animalType;

    @Builder
    public Image(String path, Long animalId, Long foundId,
                 PostStatus status, AnimalType animalType) {
        this.path = path;
        this.animalId = animalId;
        this.foundId = foundId;
        this.status = status;
        this.animalType = animalType;
        this.createdAt = LocalDateTime.now();
    }

    public void updateStatus(PostStatus status) {
        this.status = status;
    }
}
