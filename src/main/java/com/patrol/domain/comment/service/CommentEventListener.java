package com.patrol.domain.comment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.patrol.api.lostFoundPost.dto.LostFoundPostResponseDto;
import com.patrol.api.notification.dto.NotificationResponse;
import com.patrol.domain.comment.entity.Comment;
import com.patrol.domain.comment.event.CommentCreatedEvent;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.notification.entity.Notification;
import com.patrol.domain.notification.entity.NotificationType;
import com.patrol.domain.notification.repository.NotificationRepository;
import com.patrol.domain.notification.service.FCMNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventListener {

    private final NotificationRepository notificationRepository;
    private final FCMNotificationService fcmNotificationService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @EventListener
    @Transactional
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        Comment comment = event.getComment();
        LostFoundPost post = comment.getLostFoundPost();

        if (post == null) {
            log.warn("Comment {} has no associated post", comment.getId());
            return;
        }

        Member postAuthor = post.getAuthor();
        Member commentAuthor = comment.getAuthor();

        if (postAuthor.getId().equals(commentAuthor.getId())) {
            return;
        }

        try {
            String title = "새 댓글이 달렸습니다.";
            String content = String.format(
                    "%s님이 회원님의 게시글 '%s'에 댓글을 남겼습니다: %s",
                    commentAuthor.getNickname(),
                    post.getContent(),
                    comment.getContent().length() > 30
                            ? comment.getContent().substring(0, 30) + "..."
                            : comment.getContent()
            );

            Notification notification = Notification.builder()
                    .title(title)
                    .body(content)
                    .type(NotificationType.COMMENT)
                    .lostFoundPost(post)
                    .recipient(postAuthor)
                    .relatedId(comment.getId())
                    .isRead(false)
                    .build();

            Notification savedNotification = notificationRepository.save(notification);

            NotificationResponse notificationMessage = NotificationResponse.builder()
                    .id(savedNotification.getId())
                    .title(title)
                    .body(content)
                    .type(NotificationType.COMMENT)
                    .post(LostFoundPostResponseDto.from(post))
                    .relatedId(comment.getId())
                    .isRead(false)
                    .userId(postAuthor.getId())
                    .createdAt(LocalDateTime.now())
                    .build();

            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            String notificationJson = objectMapper.writeValueAsString(notificationMessage);
            kafkaTemplate.send("real-time-notification", notificationJson);
            log.info("Sending FCM notification with token: {}", fcmNotificationService.getToken(postAuthor.getEmail()));
            Message message = Message.builder()
                    .putData("postId", post.getId().toString())
                    .putData("commentId", comment.getId().toString())
                    .putData("notificationId", savedNotification.getId().toString())
                    .putData("type", NotificationType.COMMENT.name())
                    .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(content)
                                    .build()
                    )
                    .setToken(fcmNotificationService.getToken(postAuthor.getEmail()))
                    .build();

            fcmNotificationService.send(message);
            log.info("Notification sent for comment {}", comment.getId());

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification for comment {}: {}", comment.getId(), e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Failed to create notification JSON for comment {}: {}", comment.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Error in comment notification process: {}", e.getMessage());
        }
    }
}