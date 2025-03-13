package com.patrol.api.comment.dto;


import com.patrol.domain.comment.entity.Comment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommentResponseDto {
    private Long id;
    private String content;
    private String nickname;  // ✅ 추가된 필드 (작성자 닉네임)
    private Long lostFoundPostId;
    private Long userId;  // ✅ 추가된 필드 (작성자의 ID)
    private LocalDateTime createdAt;
    private String profileImage;

    public CommentResponseDto(Comment comment) {
        this.id = comment.getId();
        this.userId = comment.getAuthor().getId();  // ✅ 유저 ID 추가
        this.content = comment.getContent();
        this.nickname = comment.getAuthor().getNickname();  // ✅ Member에서 nickname 가져오기
        this.lostFoundPostId = comment.getLostFoundPost() != null ? comment.getLostFoundPost().getId() : null;
        this.createdAt = comment.getCreatedAt();
        this.profileImage = comment.getAuthor().getProfileImageUrl();
    }
}
