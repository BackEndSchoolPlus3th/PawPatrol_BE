package com.patrol.domain.ai.entity;

import com.patrol.domain.animal.enums.AnimalType;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class AiImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String path;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Enumerated(EnumType.STRING)
    private PostStatus status;

    @Enumerated(EnumType.STRING)
    private AnimalType animalType;

    @OneToOne
    @JoinColumn(name = "lost_found_post_id", nullable = false)
    private LostFoundPost lostFoundPost;

}
