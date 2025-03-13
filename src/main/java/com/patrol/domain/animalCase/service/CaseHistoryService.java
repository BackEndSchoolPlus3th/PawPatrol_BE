package com.patrol.domain.animalCase.service;

import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.animalCase.entity.CaseHistory;
import com.patrol.domain.animalCase.enums.CaseHistoryStatus;
import com.patrol.domain.animalCase.enums.ContentType;
import com.patrol.domain.animalCase.repository.CaseHistoryRepository;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.service.MemberService;
import com.patrol.global.error.ErrorCode;
import com.patrol.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaseHistoryService {

  private final CaseHistoryRepository caseHistoryRepository;
  private final MemberService memberService;

  @Transactional
  public void addInitialLostPost(AnimalCase animalCase, ContentType contentType, Long contentId, Long memberId) {
    addHistory(animalCase, CaseHistoryStatus.INITIAL_MISSING_REPORT, contentType, contentId, memberId);
  }

  @Transactional
  public void addAdditionalLostPost(AnimalCase animalCase, ContentType contentType, Long contentId, Long memberId) {
    addHistory(animalCase, CaseHistoryStatus.ADDITIONAL_MISSING_REPORT, contentType, contentId, memberId);
  }

  @Transactional
  public void addFindPost(AnimalCase animalCase, ContentType contentType, Long contentId, Long memberId) {
    addHistory(animalCase, CaseHistoryStatus.FOUND_REPORT, contentType, contentId, memberId);
  }

  @Transactional
  public void addAnimalCase(AnimalCase animalCase, ContentType contentType, Long contentId, Long memberId) {
    addHistory(animalCase, CaseHistoryStatus.TEMP_PROTECT_REGISTERED, contentType, contentId, memberId);
  }

  @Transactional
  public void addMyPet(AnimalCase animalCase, ContentType contentType, Long contentId, Long memberId) {
    addHistory(animalCase, CaseHistoryStatus.MY_PET_REGISTERED, contentType, contentId, memberId);
  }

  public void addHistory(
      AnimalCase animalCase, CaseHistoryStatus historyStatus,
      ContentType contentType, Long contentId, Long memberId
  ) {
    Member member = memberService.findById(memberId)
        .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

    CaseHistory history = createNewHistory(animalCase, historyStatus, contentType, contentId, member.getId());
    animalCase.addHistory(history);
    caseHistoryRepository.save(history);
  }


  private CaseHistory createNewHistory(
      AnimalCase animalCase, CaseHistoryStatus historyStatus,
      ContentType contentType, Long contentId, Long memberId
  ) {
    return CaseHistory.builder()
        .animalCase(animalCase)
        .historyStatus(historyStatus)
        .contentType(contentType)
        .contentId(contentId)
        .memberId(memberId)
        .build();
  }
}
