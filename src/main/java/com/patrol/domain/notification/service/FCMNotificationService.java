package com.patrol.domain.notification.service;

import com.google.firebase.messaging.*;
import com.patrol.api.chatMessage.dto.ResponseMessage;
import com.patrol.api.notification.fcm.dto.FCMTokenDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FCMNotificationService {
    private final FCMTokenDao fcmTokenDao;

    public void sendChatNotification(ResponseMessage chatMessage, String email) throws FirebaseMessagingException {
        String token = getToken(email);

        Message message = Message.builder()
                .putData("title", "채팅이 도착했습니다. ")
                .putData("content", chatMessage.getContent())
                .putData("priority", "high")
                .putData("requireInteraction", "true")
                .putData("timestamp", String.valueOf(System.currentTimeMillis()))

                .setNotification(Notification.builder()
                        .setTitle("채팅이 도착했습니다.")
                        .setBody(chatMessage.getContent())
                        .build())

                .setToken(token)
                .setFcmOptions(FcmOptions.builder().setAnalyticsLabel("high_priority").build())
                .build();

        send(message);
    }

    public void send(Message message) throws FirebaseMessagingException {
        FirebaseMessaging.getInstance().send(message);
    }

    public String getToken(String email) {
        return fcmTokenDao.getToken(email);
    }
}
