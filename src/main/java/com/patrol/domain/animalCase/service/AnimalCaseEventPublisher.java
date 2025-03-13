package com.patrol.domain.animalCase.service;


import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animalCase.enums.CaseHistoryStatus;
import com.patrol.domain.animalCase.enums.CaseStatus;
import com.patrol.domain.animalCase.enums.ContentType;
import com.patrol.domain.animalCase.events.PostCreatedEvent;
import com.patrol.domain.animalCase.events.AnimalCaseCreatedEvent;
import com.patrol.domain.animalCase.events.ProtectionStatusChangeEvent;
import com.patrol.domain.animalCase.events.MyPetCreatedEvent;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.protection.entity.Protection;
import com.patrol.domain.protection.enums.ProtectionType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnimalCaseEventPublisher {

  private final ApplicationEventPublisher eventPublisher;


  public void createLostFoundPost(LostFoundPost lostFoundPost) {
    PostStatus status = lostFoundPost.getStatus();
    ContentType contentType;
    if (status == PostStatus.FINDING) {
      contentType = ContentType.LOSTPOST;
    } else if (status == PostStatus.SIGHTED) {
      contentType = ContentType.FINDPOST;
    } else {
      return;
    }

    eventPublisher.publishEvent(new PostCreatedEvent(
        contentType, lostFoundPost.getId(),
        lostFoundPost.getPet().getId(), lostFoundPost.getAuthor().getId()
    ));
  }


  public void acceptProtection(Protection protection, Long memberId, CaseStatus toStatus) {
    ProtectionType protectionType = protection.getProtectionType();
    CaseHistoryStatus caseHistory;
    if (protectionType == ProtectionType.TEMP_PROTECTION) {
      caseHistory = CaseHistoryStatus.TEMP_PROTECT_APPROVED;
    } else if (protectionType == ProtectionType.ADOPTION) {
      caseHistory = CaseHistoryStatus.ADOPTION_APPROVED;
    } else {
      return;
    }

    eventPublisher.publishEvent(new ProtectionStatusChangeEvent(
        protection.getId(), memberId,
        toStatus, caseHistory
    ));
  }


  public void rejectProtection(Long protectionId, Long memberId, CaseStatus toStatus) {
    eventPublisher.publishEvent(new ProtectionStatusChangeEvent(
        protectionId, memberId,
        toStatus, CaseHistoryStatus.APPLICATION_REJECTED
    ));
  }

  public void createAnimalCase(
      Member member, Animal animal, String title, String description, String location
  ) {
    eventPublisher.publishEvent(new AnimalCaseCreatedEvent(
        member, animal, title, description, location, CaseStatus.PROTECT_WAITING
    ));
  }

  public void applyProtection(Protection protection, Long memberId, CaseStatus status) {
    ProtectionType protectionType = protection.getProtectionType();
    CaseHistoryStatus caseHistory;
    if (protectionType == ProtectionType.TEMP_PROTECTION) {
      caseHistory = CaseHistoryStatus.TEMP_PROTECT_REQUEST;
    } else if (protectionType == ProtectionType.ADOPTION) {
      caseHistory = CaseHistoryStatus.ADOPTION_REQUEST;
    } else {
      return;
    }

    eventPublisher.publishEvent(new ProtectionStatusChangeEvent(
        protection.getId(), memberId,
        status, caseHistory
    ));
  }

  public void createMyPet(Member member, Animal animal) {
    eventPublisher.publishEvent(new AnimalCaseCreatedEvent(
        member, animal, null, null, null, CaseStatus.MY_PET
    ));
  }
}
