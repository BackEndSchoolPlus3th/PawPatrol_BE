package com.patrol.domain.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageEventConsumer {
    private final AiImageRepository aiImageRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "image-events", groupId = "image-embedding-processor")
    public void processImageEvent(@Payload String message) throws IOException {
        try {
            log.info("🔍🔍🔍🔍🔍 Counsumer에 도착!!!!!!!!!!!!!!!!!!!!!!!!!");
            Map<String, String> event = objectMapper.readValue(message, new TypeReference<>() {});
            Long imageId = Long.parseLong(event.get("imageId"));
            String imageUrl = event.get("imageUrl");

            log.info("🔍 AI 서버에 이미지 분석 요청: imageId={}", imageId);
            Map<String, String> embeddingData = aiClient.extractEmbeddingAndFeaturesFromUrl(imageUrl);

            if (!embeddingData.containsKey("embedding")) {
                throw new RuntimeException("🚨 임베딩 추출 실패: imageId=" + imageId);
            }

            saveEmbeddingData(embeddingData, imageId);

        } catch (Exception e) {
            log.error("🚨 Kafka 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @Transactional
    protected void saveEmbeddingData (Map<String, String> embeddingData, Long imageId) {
        AiImage aiImage = aiImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 이미지가 존재하지 않음: " + imageId));

        aiImage.setEmbedding(embeddingData.get("embedding"));
        aiImage.setFeatures(embeddingData.get("features"));
        aiImageRepository.save(aiImage);
        log.info("✅ 임베딩 데이터 저장 완료: imageId={}", imageId);
    }

}
