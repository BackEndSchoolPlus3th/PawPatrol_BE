package com.patrol.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.patrol.api.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationConsumer {
    private final SimpMessagingTemplate messagingTemplate;
    private final Logger logger = LoggerFactory.getLogger(com.patrol.domain.notification.service.NotificationConsumer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "real-time-notification", groupId = "push-notification-group")
    public void consumeNotification(String messageJson) {
        try {
            objectMapper.registerModule(new JavaTimeModule());
            NotificationResponse message = objectMapper.readValue(messageJson, NotificationResponse.class);
            String userId = message.getUserId().toString();

            messagingTemplate.convertAndSend(
                    "/queue/notification/"+ userId,
                    message
            );

        } catch (Exception e) {
            logger.error("Error sending Message: ", e);
        }
    }
}