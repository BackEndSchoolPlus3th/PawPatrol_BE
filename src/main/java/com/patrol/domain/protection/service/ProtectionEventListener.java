package com.patrol.domain.protection.service;

import com.patrol.api.chatMessage.dto.RequestMessage;
import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.animalCase.enums.CaseStatus;
import com.patrol.domain.chatMessage.service.ChatMessageService;
import com.patrol.domain.chatRoom.entity.ChatRoomType;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.protection.entity.Protection;
import com.patrol.domain.protection.enums.ProtectionType;
import com.patrol.domain.protection.event.ProtectionEvent;
import com.patrol.domain.protection.repository.ProtectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProtectionEventListener {

    private final ChatMessageService chatMessageService;
    private final ProtectionRepository protectionRepository;

    @EventListener
    @Transactional
    public void handleProtectionApplication(ProtectionEvent event) {
        System.out.println("보호 신청 이벤트 발생");

        Protection protection = protectionRepository.findById(event.getProtectionId())
                .orElseThrow(() -> new RuntimeException("보호 신청을 찾을 수 없습니다."));

        ProtectionEvent.ProtectionEventType eventType = event.getEventType();

        switch (eventType) {
            case PROTECTION_REQUESTED:
                sendApplicationMessage(protection.getId());
                break;
            case PROTECTION_APPROVED:
                sendAcceptanceMessage(protection.getId(), event.getMemberId(), protection.getAnimalCase().getStatus());
                break;
            case PROTECTION_REJECTED:
                sendRejectionMessage(protection.getId(), event.getMemberId());
                break;
            case PROTECTION_CANCELED:
                break;
            default:
                System.out.println("Unhandled protection event type: " + eventType);
        }
    }

    private void sendApplicationMessage(Long protectionId) {
        Protection protection = protectionRepository.findById(protectionId)
                .orElseThrow(() -> new RuntimeException("보호 신청을 찾을 수 없습니다."));

        AnimalCase animalCase = protection.getAnimalCase();
        Member applicant = protection.getApplicant();
        Member currentFoster = animalCase.getCurrentFoster();
        Long postId = animalCase.getId();

        String statusType = protection.getProtectionType() == ProtectionType.ADOPTION ? "입양" : "임시보호";
        String initialMessage = String.format(
                "안녕하세요! %s님께서 %s에 대한 %s 신청을 하셨습니다.\n\n" +
                        "신청 이유: %s\n" +
                        "신청 유형: %s\n\n" +
                        "추가 문의사항은 이 채팅을 통해 연락주세요.",
                applicant.getNickname(),
                animalCase.getTitle(),
                statusType,
                protection.getReason(),
                statusType
        );

        sendChatMessage(postId, applicant.getId(), currentFoster.getId(), initialMessage);
    }

    private void sendAcceptanceMessage(Long protectionId, Long memberId, CaseStatus newStatus) {
        Protection protection = protectionRepository.findById(protectionId)
                .orElseThrow(() -> new RuntimeException("보호 신청을 찾을 수 없습니다."));

        AnimalCase animalCase = protection.getAnimalCase();
        Member applicant = protection.getApplicant();
        Member currentFoster = animalCase.getCurrentFoster();
        Long postId = animalCase.getId();

        String statusType = protection.getProtectionType() == ProtectionType.ADOPTION ? "입양" : "임시보호";
        String acceptanceMessage = String.format(
                "%s님, 축하합니다! %s에 대한 %s 신청이 수락되었습니다.\n\n" +
                        "동물 정보: %s\n" ,
                applicant.getNickname(),
                animalCase.getTitle(),
                statusType,
                animalCase.getAnimal().getName()
        );

        sendChatMessage(postId, currentFoster.getId(), applicant.getId(), acceptanceMessage);
    }

    private void sendRejectionMessage(Long protectionId, Long memberId) {
        Protection protection = protectionRepository.findById(protectionId)
                .orElseThrow(() -> new RuntimeException("보호 신청을 찾을 수 없습니다."));

        AnimalCase animalCase = protection.getAnimalCase();
        Member applicant = protection.getApplicant();
        Member currentFoster = animalCase.getCurrentFoster();
        Long postId = animalCase.getId();

        String statusType = protection.getProtectionType() == ProtectionType.ADOPTION ? "입양" : "임시보호";
        String rejectionMessage = String.format(
                "%s님, %s에 대한 %s 신청이 아쉽게도 거절되었습니다.\n\n" +
                        "거절 사유: %s\n\n",
                applicant.getNickname(),
                animalCase.getTitle(),
                statusType,
                protection.getRejectReason() != null ? protection.getRejectReason() : "명시된 이유 없음"
        );

        sendChatMessage(postId, currentFoster.getId(), applicant.getId(), rejectionMessage);
    }
    private void sendChatMessage(Long postId, Long senderId, Long receiverId, String content) {
        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setContent(content);
        requestMessage.setSenderId(senderId);
        requestMessage.setReceiverId(receiverId);
        System.out.println("채팅 메시지 전송: " + content.substring(0, Math.min(content.length(), 30)) + "...");
        chatMessageService.writeMessage(postId, requestMessage, ChatRoomType.PROTECTADOPT);
    }
}