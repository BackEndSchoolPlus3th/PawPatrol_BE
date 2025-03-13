package com.patrol.domain.member.auth.entity;



import com.patrol.api.member.member.dto.OAuthProviderStatus;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.ProviderType;
import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "oauth_providers", uniqueConstraints = {
    @UniqueConstraint(columnNames = "kakao_provider_id"),
    @UniqueConstraint(columnNames = "google_provider_id"),
    @UniqueConstraint(columnNames = "naver_provider_id"),
})
public class OAuthProvider extends BaseEntity {

  @MapsId
  @OneToOne
  @JoinColumn(name = "member_id")
  @EqualsAndHashCode.Include
  private Member member;


  // 소셜 계정 정보
  @Embedded
  private KakaoProvider kakao;

  public void addKakaoProvider(String providerId, String email) {
    KakaoProvider kakaoProvider = KakaoProvider.builder()
        .providerId(providerId)
        .email(email)
        .build();
    kakaoProvider.connect();
    kakao = kakaoProvider;
  }

  public void removeKakaoProvider() {
    if (kakao != null) {
      kakao.disconnect();
      kakao = null;
    }
  }


  @Embedded
  private GoogleProvider google;

  public void addGoogleProvider(String providerId, String email) {
    GoogleProvider googleProvider = GoogleProvider.builder()
        .providerId(providerId)
        .email(email)
        .build();
    googleProvider.connect();
    google = googleProvider;
  }

  public void removeGoogleProvider() {
    if (google != null) {
      google.disconnect();
      google = null;
    }
  }


  @Embedded
  private NaverProvider naver;

  public void addNaverProvider(String providerId, String email) {
    NaverProvider naverProvider = NaverProvider.builder()
        .providerId(providerId)
        .email(email)
        .build();
    naverProvider.connect();
    naver = naverProvider;
  }

  public void removeNaverProvider() {
    if (naver != null) {
      naver.disconnect();
      naver = null;
    }
  }

  @Builder
  private OAuthProvider(Member member) {
    this.member = member;
  }

  public boolean isConnected(ProviderType type) {
    return switch (type) {
      case KAKAO -> kakao != null && kakao.isConnected();
      case GOOGLE -> google != null && google.isConnected();
      case NAVER -> naver != null && naver.isConnected();
      case SELF -> false;
    };
  }

  public Map<ProviderType, OAuthProviderStatus> getOAuthProviderStatuses() {
    Map<ProviderType, OAuthProviderStatus> statuses = new HashMap<>();

    // kakao 정보 추가
    statuses.put(ProviderType.KAKAO, OAuthProviderStatus.builder()
        .createDate(kakao != null ? kakao.getConnectedAt() : null)
        .email(kakao != null ? kakao.getEmail() : null)
        .active(kakao != null && kakao.isConnected())
        .build());

    // google 정보 추가
    statuses.put(ProviderType.GOOGLE, OAuthProviderStatus.builder()
        .createDate(google != null ? google.getConnectedAt() : null)
        .email(google != null ? google.getEmail() : null)
        .active(google != null && google.isConnected())
        .build());

    // naver 정보 추가
    statuses.put(ProviderType.NAVER, OAuthProviderStatus.builder()
        .createDate(naver != null ? naver.getConnectedAt() : null)
        .email(naver != null ? naver.getEmail() : null)
        .active(naver != null && naver.isConnected())
        .build());

    return statuses;
  }

}
