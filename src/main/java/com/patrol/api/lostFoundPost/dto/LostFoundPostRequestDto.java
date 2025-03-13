package com.patrol.api.lostFoundPost.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LostFoundPostRequestDto {
    private String content;
    private Double latitude;
    private Double longitude;
    private String location;
    private String findTime;
    private String lostTime;
    private String status;
    private Long petId;
    private String animalType;
    private Integer reward;

}
