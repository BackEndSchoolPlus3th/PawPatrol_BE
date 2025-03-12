package com.patrol.domain.lostFoundPost.service;

import com.patrol.api.lostFoundPost.dto.LostFoundPostResponseDto;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.member.member.entity.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.patrol.domain.comment.entity.Comment;
import java.util.HashMap;
import java.util.Map;



@Service
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // 전체 알림 전송 메소드 (기존 코드)
    public void sendLostFoundPostNotification(LostFoundPost lostFoundPost) {
        LostFoundPostResponseDto responseDto = LostFoundPostResponseDto.from(lostFoundPost);

        // 클라이언트로 알림을 보내는 부분. /topic/lost-found-posts 에 메시지 전송
        messagingTemplate.convertAndSend("/topic/lost-found-posts", responseDto);
    }

    // 특정 사용자에게만 알림 전송 메소드 (수정)
    public void sendPersonalLostFoundPostNotification(LostFoundPost lostFoundPost, Member targetMember, Comment comment) {
        LostFoundPostResponseDto responseDto = LostFoundPostResponseDto.from(lostFoundPost);

        // 알림에 추가 정보 설정
        Map<String, Object> notification = new HashMap<>();
        notification.put("postData", responseDto);
        notification.put("type", "COMMENT");
        notification.put("message", "회원님의 게시글에 새로운 댓글이 작성되었습니다.");
        notification.put("postId", lostFoundPost.getId());

        // 댓글 및 작성자 정보 추가
        notification.put("commentId", comment.getId());
        notification.put("commentContent", comment.getContent());
        notification.put("commentAuthorId", comment.getAuthor().getId());
        notification.put("commentAuthorNickname", comment.getAuthor().getNickname());


        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        // targetMember의 ID를 사용하여 해당 사용자에게만 알림 전송
        messagingTemplate.convertAndSendToUser(
                targetMember.getId().toString(),
                "/queue/notifications",
                notification
        );
    }
}