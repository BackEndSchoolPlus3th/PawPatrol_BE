package com.patrol.domain.member.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SocialConnectService {

  private final RedisTemplate<String, String> redisTemplate;
  private static final String SOCIAL_CONNECT_KEY = "social:connect";
  private static final long EXPIRE_TIME = 5 * 60;


  public void storeOrigin(Long memberId) {
    redisTemplate.opsForValue().set(
        SOCIAL_CONNECT_KEY,
        memberId.toString(),
        EXPIRE_TIME,
        TimeUnit.SECONDS)
    ;
  }

  public Long getOrigin() {
    String value = redisTemplate.opsForValue().get(SOCIAL_CONNECT_KEY);
    return value != null ? Long.parseLong(value) : null;
  }

  public void removeOrigin() {
    redisTemplate.delete(SOCIAL_CONNECT_KEY);
  }

}
