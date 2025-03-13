package com.patrol.domain.animalCase.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseStatus {

  MY_PET("마이펫"),
  UNKNOWN("불명"),
  MISSING("실종"),
  SHELTER_PROTECTING("보호소 보호 중"),
  OWNER_FOUND("보호자 인계 완료"),

  PROTECT_WAITING("임시보호 대기"),
  TEMP_PROTECTING("임시보호 중"),
  ADOPTED("입양 완료");


  private final String description;

  public boolean isTempProtectible() {
    return this == PROTECT_WAITING || this == TEMP_PROTECTING;
  }
}
