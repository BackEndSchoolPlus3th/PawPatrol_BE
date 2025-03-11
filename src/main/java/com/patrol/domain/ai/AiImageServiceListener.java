package com.patrol.domain.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiImageServiceListener {
    private final AiImageRepository aiImageRepository;
    private final ImageEventProducer imageEventProducer;

    @TransactionalEventListener
    public void onAiImageSaved(AiImageSavedEvent event) {
        AiImage image = event.getAiImage();

        if (!aiImageRepository.existsByEmbeddingIsNotNullOrFeaturesIsNotNullAndPath(image.getPath())) {
            log.info("📌 임베딩되지 않은 이미지 발견. Kafka 이벤트 전송: {}", image.getPath());
            imageEventProducer.sendImageEvent(image.getId(), image.getPath());
        } else {
            log.info("✅ 이미 임베딩된 이미지: {}", image.getId());
        }
    }
}
