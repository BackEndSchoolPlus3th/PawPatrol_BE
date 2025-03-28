package com.patrol.domain.member.member.enums;


import com.patrol.global.exceptions.ErrorCodes;
import com.patrol.global.exceptions.ServiceException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ProviderType {
  SELF("일반"),
  KAKAO("카카오"),
  GOOGLE("구글"),
  NAVER("네이버");

  private final String displayName;

  public static ProviderType of(String type) {
    try {
      return ProviderType.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ServiceException(ErrorCodes.TYPE_TRANSFER_FAILED);
    }
  }
}
