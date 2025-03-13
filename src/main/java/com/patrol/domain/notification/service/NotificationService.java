package com.patrol.domain.notification.service;

import com.patrol.api.notification.dto.NotificationResponse;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.notification.entity.Notification;
import com.patrol.domain.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Member member, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(member, pageable);
        return notifications.map(NotificationResponse::from);
    }

    @Transactional
    public void markAsRead(Long notificationId, Member member) {
        Notification notification = findNotificationByIdAndMember(notificationId, member);
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private Notification findNotificationByIdAndMember(Long notificationId, Member member) {
        return notificationRepository.findById(notificationId)
                .filter(notification -> notification.getRecipient().getId().equals(member.getId()))
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));
    }

    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("해당 알림을 찾을 수 없습니다."));

        notificationRepository.delete(notification);
    }
}