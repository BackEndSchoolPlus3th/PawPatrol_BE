package com.patrol.api.lostFoundPost.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LostFoundPostRequestDto {
    private String content;
    private Double latitude;  // 위도 추가
    private Double longitude; // 경도 추가
    private String location;
    private String findTime;
    private String lostTime;
    private String status; // 상태를 String으로 받음
    private Long petId;  // Add petId here
    private String animalType; // Add animalType here to capture the animal type
    private Integer reward;

}
