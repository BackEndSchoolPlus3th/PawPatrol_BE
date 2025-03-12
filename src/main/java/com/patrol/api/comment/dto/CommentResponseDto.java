package com.patrol.api.comment.dto;


import com.patrol.domain.comment.entity.Comment;
import lombok.Getter;

@Getter
public class CommentResponseDto {
    private Long id;
    private String content;
    private String nickname;
    private Long lostFoundPostId;
    private Long userId;

    public CommentResponseDto(Comment comment) {
        this.id = comment.getId();
        this.userId = (comment.getAuthor() != null) ? comment.getAuthor().getId() : null;
        this.content = comment.getContent();
        this.nickname = comment.getAuthor().getNickname() != null ? comment.getAuthor().getNickname() : null;
        this.lostFoundPostId = comment.getLostFoundPost() != null ? comment.getLostFoundPost().getId() : null;
    }
}
