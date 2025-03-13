package com.patrol.domain.ai.service;

import com.patrol.domain.ai.event.AiImageSavedEvent;
import com.patrol.domain.ai.event.ImageEventProducer;
import com.patrol.domain.ai.entity.AiImage;
import com.patrol.domain.ai.repository.AiImageRepository;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
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

        if (!aiImageRepository.existsByEmbeddingIsNotNullOrFeaturesIsNotNullAndId(image.getId())
                || image.getStatus() == PostStatus.SIGHTED) {
            log.info("🔍 AI 서버에 이미지 분석 요청: imageId={}", image.getId());
            imageEventProducer.sendImageEvent(image.getId(), image.getPath());
        } else {
            log.info("✅ 이미 임베딩된 이미지: {}", image.getId());
        }
    }
}
