package com.patrol.domain.animalCase.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseHistoryStatus {

  MY_PET_REGISTERED("마이펫 등록"),
  INITIAL_MISSING_REPORT("최초 실종 신고"),
  ADDITIONAL_MISSING_REPORT("추가 실종 신고"),
  FOUND_REPORT("발견 제보"),

  // 임시보호
  TEMP_PROTECT_REGISTERED("임시보호/입양 등록"),
  TEMP_PROTECT_WAIT("임시보호/입양 대기"),
  TEMP_PROTECT_REQUEST("임시보호 신청"),
  ADOPTION_REQUEST("입양 신청"),
  TEMP_PROTECT_APPROVED("임시보호 승인"),
  ADOPTION_APPROVED("입양 승인"),
  APPLICATION_REJECTED("신청 거절"),
  TEMP_PROTECT_END("임시보호 종료"),

  // 종료 관련
  OWNER_FOUND("보호자 인계"),
  ADOPTED("입양 완료");

  private final String description;

}
