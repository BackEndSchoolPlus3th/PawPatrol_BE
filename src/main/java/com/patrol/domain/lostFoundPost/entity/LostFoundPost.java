package com.patrol.domain.lostFoundPost.entity;

import com.patrol.api.lostFoundPost.dto.LostFoundPostRequestDto;
import com.patrol.domain.Postable.Postable;
import com.patrol.domain.ai.entity.AiImage;
import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animal.enums.AnimalType;
import com.patrol.domain.comment.entity.Comment;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lost_found_post")
public class LostFoundPost extends BaseEntity implements Postable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Member author;

    @OneToMany(mappedBy = "foundId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();


    @OneToMany(mappedBy = "lostFoundPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pet_id", nullable = true)
    private Animal pet;

    @Enumerated(EnumType.STRING)
    private AnimalType animalType;

    @OneToOne(mappedBy = "lostFoundPost", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AiImage aiImage;

    private String title;
    private String content;
    private Double latitude;
    private Double longitude;
    private String location;
    private String findTime;
    private String lostTime;
    @Enumerated(EnumType.STRING)
    private PostStatus status;
    private Integer reward;

    public LostFoundPost(LostFoundPostRequestDto requestDto, Member author) {
        this(requestDto);
        this.author = author;
    }

    public LostFoundPost(LostFoundPostRequestDto requestDto, Member author, Animal pet, AnimalType animalType) {
        this(requestDto);
        this.author = author;
        this.pet = pet;  // null로 전달되면 null로 유지됨
        this.animalType = animalType != null ? animalType : null;
        this.pet = pet;
        this.animalType = animalType != null ? animalType : null;
    }

    public LostFoundPost(LostFoundPostRequestDto requestDto) {
        this.content = requestDto.getContent();
        this.latitude = requestDto.getLatitude();
        this.longitude = requestDto.getLongitude();
        this.location = requestDto.getLocation();
        if (requestDto.getLostTime() != null && requestDto.getFindTime() == null) {
            this.lostTime = requestDto.getLostTime();
            this.findTime = null;
        } else if (requestDto.getFindTime() != null && requestDto.getLostTime() == null) {
            this.findTime = requestDto.getFindTime();
            this.lostTime = null;
        } else {
            this.findTime = null;
            this.lostTime = null;
        }
        if (requestDto.getStatus() != null) {
            this.status = PostStatus.valueOf(requestDto.getStatus());
        } else {
            this.status = PostStatus.FINDING;
        }

        this.animalType = requestDto.getAnimalType() != null ? AnimalType.valueOf(requestDto.getAnimalType()) : null;  // Set animalType
        this.reward = requestDto.getReward();
    }

    public LostFoundPost (LostFoundPostRequestDto requestDto, Member author, Animal pet) {
        this(requestDto, author);
        this.pet = pet;
    }

    public void addImage(Image image) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.add(image);
    }
    @Override
    public Long getId() {
        return super.getId();
    }

    @Override
    public String getPostType() {
        return "LOSTFOUND";
    }
    public void addComment (String comment) {
        Comment newComment = new Comment(comment, this);
        this.comments.add(newComment);
    }
}
