package com.patrol.domain.ai.event;

import com.patrol.domain.ai.entity.AiImage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AiImageSavedEvent {
    private final AiImage aiImage;

}
