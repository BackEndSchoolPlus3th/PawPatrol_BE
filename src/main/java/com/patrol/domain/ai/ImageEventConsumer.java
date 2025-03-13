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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageEventConsumer {
    private final AiImageRepository aiImageRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final ImageProcessingService imageProcessingService;

    @KafkaListener(
            topics = "image-events",
            groupId = "${spring.kafka.groups.ai-group-id}"
    )
    public void processImageEvent(@Payload String message) throws IOException {
        try {
            log.info("🔍 Consumer received message: {}", message);
            log.info("🔍🔍🔍🔍🔍 Counsumer에 도착!!!!!!!!!!!!!!!!!!!!!!!!!");
            log.error("🚨🚨🚨 KAFKA LISTENER ACTIVATED 🚨🚨🚨");
            log.error("🚨 Received message: {}", message);
            Map<String, String> event = objectMapper.readValue(message, new TypeReference<>() {});
            Long imageId = Long.parseLong(event.get("imageId"));
            String imageUrl = event.get("imageUrl");

            log.info("🔍 AI 서버에 이미지 분석 요청: imageId={}", imageId);
            Map<String, String> embeddingData = aiClient.extractEmbeddingAndFeaturesFromUrl(imageUrl);

            if (!embeddingData.containsKey("embedding")) {
                throw new RuntimeException("🚨 임베딩 추출 실패: imageId=" + imageId);
            }

            PostStatus postStatus = saveEmbeddingData(embeddingData, imageId);

            if (postStatus == PostStatus.FINDING) {
                log.info("📩 FINDING 이미지 유사도 분석 요청: imageId={}", imageId);
                imageProcessingService.asyncProcessImageFind(imageId);
            } else if (postStatus == PostStatus.SIGHTED) {
                log.info("📩 SIGHTED 이미지 유사도 분석 요청: imageId={}", imageId);
                imageProcessingService.asyncProcessSightedImage(imageId);
            }


        } catch (Exception e) {
            log.error("🚨 Kafka 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @Transactional
    protected PostStatus saveEmbeddingData (Map<String, String> embeddingData, Long imageId) {
        AiImage aiImage = aiImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 이미지가 존재하지 않음: " + imageId));

        aiImage.setEmbedding(embeddingData.get("embedding"));
        aiImage.setFeatures(embeddingData.get("features"));
        aiImageRepository.save(aiImage);
        log.info("✅ 임베딩 데이터 저장 완료: imageId={}", imageId);
        return aiImage.getStatus();
    }

}
