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

  public void connectProvider(Member member, ProviderType type, String providerId, String email) {
    OAuthProviderStrategy strategy = strategies.get(type);
    if (strategy == null) {
      throw new ServiceException(ErrorCodes.INVALID_LOGIN_TYPE);
    }
    strategy.connect(member, providerId, email);
  }

  public void disconnectProvider(Member member, ProviderType type) {
    OAuthProviderStrategy strategy = strategies.get(type);
    if (strategy == null) {
      throw new ServiceException(ErrorCodes.INVALID_LOGIN_TYPE);
    }
    strategy.disconnect(member);
  }

  @Transactional
  public Member findByProviderId(ProviderType type, String providerId) {
    OAuthProviderStrategy strategy = strategies.get(type);
    if (strategy == null) {
      throw new ServiceException(ErrorCodes.INVALID_LOGIN_TYPE);
    }
    return strategy.findByProviderId(providerId);
  }
}
