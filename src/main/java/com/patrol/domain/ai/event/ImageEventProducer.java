package com.patrol.domain.ai.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "image-events"; // Kafka 토픽 이름
    private static final int MAX_RETRY_ATTEMPTS = 3;  // 최대 재시도 횟수
    private static final long RETRY_DELAY_MS = 5000;  // 재시도 간격 5초

    public void sendImageEvent(Long imageId, String imageUrl) {
        try {
            log.info("🔍🔍🔍🔍🔍 Producer에 도착!!!!!!!!!!!!!!!!!!!!!!!!!");
            Map<String, String> event = new HashMap<>();
            event.put("imageId", imageId.toString());
            event.put("imageUrl", imageUrl);

            String eventJson = objectMapper.writeValueAsString(event);

            sendWithRetry(imageId.toString(), eventJson, 0);

        } catch (Exception e) {
            log.error("🚨 Kafka 메시지 변환 실패: {}", e.getMessage(), e);
        }
    }

    private void sendWithRetry(String key, String message, int attempt) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC, key, message);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("🚨 Kafka 이벤트 전송 실패 ({}차 시도): {}, 오류: {}", attempt + 1, message, ex.getMessage(), ex);

                // 최대 재시도 횟수 이하인 경우, 일정 시간 후 재시도
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ignored) {}

                    sendWithRetry(key, message, attempt + 1);
                } else {
                    log.error("🚨 Kafka 이벤트 전송 최종 실패: {}, 모든 재시도 완료", message);
                }
            } else {
                RecordMetadata metadata = result.getRecordMetadata();
                log.info("✅ Kafka 이벤트 전송 성공: {}, Partition: {}, Offset: {}", message, metadata.partition(), metadata.offset());
            }
        });
    }
}
