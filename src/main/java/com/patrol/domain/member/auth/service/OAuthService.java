package com.patrol.domain.member.auth.service;


import com.patrol.domain.member.auth.strategy.OAuthProviderStrategy;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.ProviderType;
import com.patrol.global.exceptions.ErrorCodes;
import com.patrol.global.exceptions.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class OAuthService {

  private final Map<ProviderType, OAuthProviderStrategy> strategies;

  public OAuthService(List<OAuthProviderStrategy> providerStrategies) {
    this.strategies = providerStrategies.stream()
        .collect(Collectors.toMap(
            OAuthProviderStrategy::getProviderType,
            strategy -> strategy
        ));
  }


  // 자체 계정에 소셜 계정 연동
  public void connectProvider(Member member, ProviderType type, String providerId, String email) {
    OAuthProviderStrategy strategy = strategies.get(type);
    if (strategy == null) {
      throw new ServiceException(ErrorCodes.INVALID_LOGIN_TYPE);
    }
    strategy.connect(member, providerId, email);
  }


  // 소셜 계정 연동 해제
  public void disconnectProvider(Member member, ProviderType type) {
    OAuthProviderStrategy strategy = strategies.get(type);
    if (strategy == null) {
      throw new ServiceException(ErrorCodes.INVALID_LOGIN_TYPE);
    }
    strategy.disconnect(member);
  }


  // ProviderId를 통해 연동 정보 찾기
  @Transactional
  public Member findByProviderId(ProviderType type, String providerId) {
    // 전략 객체 조회 type 에 해당하는 전략 객체를 찾는다.
    OAuthProviderStrategy strategy = strategies.get(type);
    // 유효성 검사
    if (strategy == null) {
      throw new ServiceException(ErrorCodes.INVALID_LOGIN_TYPE);
    }
    // 실제 전략 실행
    return strategy.findByProviderId(providerId);
  }

}
