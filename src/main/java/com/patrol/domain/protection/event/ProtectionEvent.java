package com.patrol.domain.protection.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProtectionEvent {
    private final Long protectionId;
    private final Long animalCaseId;
    private final Long memberId;
    private final ProtectionEventType eventType;

    public enum ProtectionEventType {
        PROTECTION_REQUESTED,
        PROTECTION_APPROVED,
        PROTECTION_REJECTED,
        PROTECTION_CANCELED
    }
}
