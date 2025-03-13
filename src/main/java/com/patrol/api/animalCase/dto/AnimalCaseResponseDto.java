package com.patrol.api.animalCase.dto;

import com.patrol.api.PostResponseDto.PostResponseDto;
import com.patrol.domain.animalCase.entity.AnimalCase;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnimalCaseResponseDto implements PostResponseDto {
    private Long id;
    private String content;

    public AnimalCaseResponseDto(AnimalCase post) {
        this.id = post.getId();
        this.content = post.getTitle();
    }

    @Override
    public String getPostType() {
        return "ANIMALCASE";
    }
}
