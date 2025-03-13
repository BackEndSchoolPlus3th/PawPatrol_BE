package com.patrol.domain.protection.event;

import com.patrol.domain.animalCase.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProtectionEvent {
    private final Long protectionId;        // ID of the Protection entity
    private final Long animalCaseId;        // ID of the associated AnimalCase
    private final Long memberId;            // ID of the member who initiated the event
    private final ProtectionEventType eventType; // Type of the protection event

    // Enum for different types of protection events
    public enum ProtectionEventType {
        PROTECTION_REQUESTED,
        PROTECTION_APPROVED,
        PROTECTION_REJECTED,
        PROTECTION_CANCELED
    }
}