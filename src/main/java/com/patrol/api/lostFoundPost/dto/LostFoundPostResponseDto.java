package com.patrol.api.lostFoundPost.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.patrol.api.PostResponseDto.PostResponseDto;
import com.patrol.api.animal.dto.PetResponseDto;
import com.patrol.api.image.dto.ImageResponseDto;
import com.patrol.api.member.member.dto.MemberResponseDto;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LostFoundPostResponseDto implements PostResponseDto {
    private Long id;
    private MemberResponseDto author;
    private Long userId;
    private String nickname;
    private String content;
    private Double latitude;
    private Double longitude;
    private String findTime;
    private String location;
    private String lostTime;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private String status;
    private PetResponseDto pet;
    private List<ImageResponseDto> images;
    private String animalType;
    private Integer reward;
    //private Long petId;  // petId 추가
    // Add petId to the ResponseDto
    //private Long petId;

    public LostFoundPostResponseDto(LostFoundPost lostFoundPost) {
        this.id = lostFoundPost.getId();
        this.author = new MemberResponseDto(lostFoundPost.getAuthor());
        this.userId = lostFoundPost.getAuthor().getId();
        this.nickname = lostFoundPost.getAuthor().getNickname();
        this.content = lostFoundPost.getContent();
        this.latitude = lostFoundPost.getLatitude();
        this.longitude = lostFoundPost.getLongitude();
        this.findTime = lostFoundPost.getFindTime();
        this.lostTime = lostFoundPost.getLostTime();
        if (lostFoundPost.getPet() != null && lostFoundPost.getPet().getAnimalType() != null) {
            this.animalType = lostFoundPost.getPet().getAnimalType().toString();
        } else {
            this.animalType = (lostFoundPost.getAnimalType() != null && !lostFoundPost.getAnimalType().toString().equals("null"))
                    ? lostFoundPost.getAnimalType().toString()
                    : "null";
        }
        this.status= String.valueOf(lostFoundPost.getStatus());
        this.location = lostFoundPost.getLocation();
        this.createdAt = lostFoundPost.getCreatedAt();
        this.modifiedAt = lostFoundPost.getModifiedAt();
        this.pet = lostFoundPost.getPet() != null ? new PetResponseDto(lostFoundPost.getPet()) : null;

        this.images = lostFoundPost.getImages().stream()
                .map(ImageResponseDto::new)
                .collect(Collectors.toList());

        this.reward = lostFoundPost.getReward();
    }
    public static LostFoundPostResponseDto from(LostFoundPost lostFoundPost) {
        return new LostFoundPostResponseDto(lostFoundPost);
    }


    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getPostType() {
        return "LOSTFOUND";
    }
}
