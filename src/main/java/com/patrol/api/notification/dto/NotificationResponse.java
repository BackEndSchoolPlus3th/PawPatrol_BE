package com.patrol.api.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.patrol.api.PostResponseDto.PostResponseDto;
import com.patrol.api.lostFoundPost.dto.LostFoundPostResponseDto;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.notification.entity.Notification;
import com.patrol.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private String title;
    private String body;
    private NotificationType type;
    private LostFoundPostResponseDto post;
    private Long relatedId;
    @JsonProperty("read")
    private boolean isRead;
    private LocalDateTime createdAt;
    private Long userId;

    public static NotificationResponse from(Notification notification) {
        LostFoundPost lostFoundPost = notification.getLostFoundPost();
        LostFoundPostResponseDto postDto = lostFoundPost != null ? LostFoundPostResponseDto.from(lostFoundPost) : null;

        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .post(postDto)
                .relatedId(notification.getRelatedId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .userId(notification.getRecipient().getId())
                .build();
    }
}