package com.patrol.domain.protection.service;

import com.patrol.domain.animalCase.enums.CaseStatus;
import com.patrol.domain.animalCase.enums.ContentType;
import com.patrol.domain.protection.entity.Protection;
import com.patrol.domain.protection.event.ProtectionEvent;
import com.patrol.domain.protection.event.ProtectionEvent.ProtectionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProtectionEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void applyProtection(Protection protection, Long memberId) {
        ProtectionEvent event = new ProtectionEvent(
                protection.getId(),
                protection.getAnimalCase().getId(),
                memberId,
                ProtectionEventType.PROTECTION_REQUESTED
        );
        eventPublisher.publishEvent(event);
    }

    public void acceptProtection(Protection protection, Long memberId) {
        ProtectionEvent event = new ProtectionEvent(
                protection.getId(),
                protection.getAnimalCase().getId(),
                memberId,
                ProtectionEventType.PROTECTION_APPROVED
        );
        eventPublisher.publishEvent(event);
    }

    public void rejectProtection(Long protectionId, Long memberId) {
        ProtectionEvent event = new ProtectionEvent(
                protectionId,
                null,
                memberId,
                ProtectionEventType.PROTECTION_REJECTED
        );
        eventPublisher.publishEvent(event);
    }

    public void cancelProtection(Protection protection, Long memberId) {
        ProtectionEvent event = new ProtectionEvent(
                protection.getId(),
                protection.getAnimalCase().getId(),
                memberId,
                ProtectionEventType.PROTECTION_CANCELED
        );
        eventPublisher.publishEvent(event);
    }
}
