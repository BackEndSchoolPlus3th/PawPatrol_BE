package com.patrol.domain.animalCase.events;

import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animal.service.AnimalService;
import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.animalCase.enums.CaseStatus;
import com.patrol.domain.animalCase.enums.ContentType;
import com.patrol.domain.animalCase.service.AnimalCaseService;
import com.patrol.domain.animalCase.service.CaseHistoryService;
import com.patrol.domain.member.member.enums.MemberRole;
import com.patrol.domain.protection.entity.Protection;
import com.patrol.domain.protection.service.ProtectionService;
import com.patrol.global.error.ErrorCode;
import com.patrol.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnimalCaseEventManager {

  private final AnimalCaseService animalCaseService;
  private final CaseHistoryService caseHistoryService;
  private final AnimalService animalService;
  private final ProtectionService protectionService;


  @Transactional
  public void handleLostPost(PostCreatedEvent event) {
    Animal animal = animalService.findById(event.getAnimalId())
        .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

    AnimalCase animalCase = getOrCreateAnimalCase(animal, CaseStatus.MISSING);
    boolean isNewCase = animalCase.getId() == null;
    if (isNewCase) {
      caseHistoryService.addInitialLostPost(
          animalCase, event.getContentType(), event.getLostFoundPostId(), event.getMemberId()
      );
    } else {
      caseHistoryService.addAdditionalLostPost(
          animalCase, event.getContentType(), event.getLostFoundPostId(), event.getMemberId()
      );
    }
  }


  @Transactional
  public void handleFindPost(PostCreatedEvent event) {
    Animal animal = animalService.findById(event.getAnimalId())
        .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

    AnimalCase animalCase = getOrCreateAnimalCase(animal, CaseStatus.UNKNOWN);
    caseHistoryService.addFindPost(
        animalCase, event.getContentType(), event.getLostFoundPostId(), event.getMemberId()
    );
  }


  private AnimalCase getOrCreateAnimalCase(Animal animal, CaseStatus status) {
    AnimalCase animalCase = animalCaseService.findByAnimal(animal);
    if (animalCase == null) {
      animalCase = animalCaseService.createNewCase(status, animal);
    }
    return animalCase;
  }


  @Transactional
  public void handleProtectionStatusChange(ProtectionStatusChangeEvent event) {
    Protection protection = protectionService.findById(event.getProtectionId())
        .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

    AnimalCase animalCase = protection.getAnimalCase();

    validateStatusTransition(animalCase.getStatus(), event.getToStatus());

    animalCase.updateStatus(event.getToStatus());
    caseHistoryService.addHistory(
        animalCase, event.getHistoryStatus(), ContentType.PROTECTION, protection.getId(), event.getMemberId()
    );

    if (event.getToStatus() == CaseStatus.ADOPTED) {
      animalCase.updateStatus(CaseStatus.MY_PET);
    }
  }


  private void validateStatusTransition(CaseStatus fromStatus, CaseStatus toStatus) {
    if (fromStatus == toStatus) {
      return;
    }

    boolean isValid = switch (toStatus) {
      case TEMP_PROTECTING -> fromStatus.isTempProtectible();
      default -> false;
    };

    if (!isValid) {
      throw new CustomException(ErrorCode.INVALID_STATUS_CHANGE);
    }
  }


  @Transactional
  public void handleAnimalCaseCreated(AnimalCaseCreatedEvent event) {
    CaseStatus toStatus = event.getToStatus();
    if (event.getMember().getRole() == MemberRole.ROLE_SHELTER) {
      toStatus = CaseStatus.SHELTER_PROTECTING;
    }

    AnimalCase animalCase = animalCaseService.createNewCase(toStatus, event.getAnimal());
    animalCase.setCurrentFoster(event.getMember());
    animalCase.setTitle(event.getTitle());
    animalCase.setDescription(event.getDescription());
    animalCase.setLocation(event.getLocation());
    animalCase.setShelter(event.getMember().getShelter());

    if (toStatus == CaseStatus.MY_PET) {
      caseHistoryService.addMyPet(
          animalCase, ContentType.ANIMAL_CASE, animalCase.getId(), event.getMember().getId()
      );
    } else {
      caseHistoryService.addAnimalCase(
          animalCase, ContentType.ANIMAL_CASE, animalCase.getId(), event.getMember().getId()
      );
    }
  }
}
