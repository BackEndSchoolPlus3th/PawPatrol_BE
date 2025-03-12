package com.patrol.api.lostFoundPost.dto;

import com.patrol.api.comment.dto.CommentResponseDto;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class LostFoundPostDetailResponseDto {
    private Long id;
    private String title;
    private String content;
    private List<CommentResponseDto> comments;

    public LostFoundPostDetailResponseDto(LostFoundPost lostFoundPost, List<CommentResponseDto> comments) {
        this.id = lostFoundPost.getId();
        this.title = lostFoundPost.getTitle();
        this.content = lostFoundPost.getContent();
        this.comments = comments;
    }

    public static LostFoundPostDetailResponseDto from(LostFoundPost lostFoundPost, List<CommentResponseDto> comments) {
        return new LostFoundPostDetailResponseDto(lostFoundPost, comments);
    }
}
