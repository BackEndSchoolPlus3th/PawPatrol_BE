package com.patrol.api.lostpost.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LostPostRequestDto {
    private String title;
    private String content;
    private List<String> tags;
    private String location;
    private String lostTime;
    private String ownerPhone;
    private String status;
    private Long authorId;

}
