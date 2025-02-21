package com.patrol.api.comment.dto;

import com.patrol.domain.comment.entity.Comment;
import lombok.Getter;

@Getter
public class CommentResponseDto {
    private Long id;
    private String content;
    private String nickname;  // ✅ 추가된 필드 (작성자 닉네임)
    private Long lostPostId;
    private Long findPostId;

    public CommentResponseDto(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.nickname = comment.getAuthor().getNickname();  // ✅ Member에서 nickname 가져오기
        this.lostPostId = comment.getLostPost() != null ? comment.getLostPost().getId() : null;
        this.findPostId = comment.getFindPost() != null ? comment.getFindPost().getId() : null;
    }
}
