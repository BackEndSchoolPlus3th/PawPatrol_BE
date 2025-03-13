package com.patrol.domain.notification.repository;

import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(Member recipient, Pageable pageable);
}
