package com.patrol.domain.member.member.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.patrol.api.member.member.dto.OAuthProviderStatus;
import com.patrol.api.member.member.dto.request.MemberUpdateRequest;
import com.patrol.domain.facility.entity.Shelter;
import com.patrol.domain.member.auth.entity.OAuthProvider;
import com.patrol.domain.member.member.enums.Gender;
import com.patrol.domain.member.member.enums.MemberRole;
import com.patrol.domain.member.member.enums.MemberStatus;
import com.patrol.domain.member.member.enums.ProviderType;
import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@DynamicInsert
@Table(name = "members")
public class Member extends BaseEntity {


    @Column(unique = true, nullable = false, length = 30)
    private String email;

    @JsonIgnore
    private String password;
    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

//    @Column(nullable = false)
    private String nickname;  // 닉네임

    private LocalDate birthDate;

    private String address;  // 주소

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Gender gender;  // 성별

    @Column(length = 20)
    private String phoneNumber;  // 전화번호

    @Column(columnDefinition = "VARCHAR(255) DEFAULT 'default.png'")
    private String profileImageUrl = "default.png";  // 프로필 사진 경로

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean marketingAgree;  // 마케팅 수신여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('ACTIVE', 'INACTIVE', 'BANNED', 'WITHDRAWN') DEFAULT 'ACTIVE'")
    private MemberStatus status;  // 회원 상태
    public void deactivate() {
        status = MemberStatus.WITHDRAWN;
    }
    public void restore() {
        status = MemberStatus.ACTIVE;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SHELTER') DEFAULT 'ROLE_USER'")
    private MemberRole role = MemberRole.ROLE_USER;  // 권한 (관리자, 사용자, 보호소)
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role != null) {
            return Collections.singletonList(new SimpleGrantedAuthority(this.role.name()));
        } else {
            return Collections.singletonList(new SimpleGrantedAuthority(MemberRole.ROLE_USER.name()));
        }
    }

    @JsonIgnore
    @Column(unique = true, length = 50)
    private String apiKey;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'SELF'")
    private ProviderType loginType = ProviderType.SELF;  // 현재 로그인 방식

    // 1대1 관계 설정
    @JsonIgnore
    @OneToOne(mappedBy = "member", cascade = ALL, orphanRemoval = true)
    private OAuthProvider oAuthProvider;

    public OAuthProvider getOAuthProviderOrCreate() {
        if (oAuthProvider == null) {
            oAuthProvider = OAuthProvider.builder()
                .member(this)
                .build();
        }
        return oAuthProvider;
    }

    public boolean hasOAuthProvider(ProviderType type) {
        return oAuthProvider != null && oAuthProvider.isConnected(type);
    }

    public int getConnectedOAuthCount() {
        if (oAuthProvider == null) return 0;

        int count = 0;
        if (oAuthProvider.isConnected(ProviderType.KAKAO)) count++;
        if (oAuthProvider.isConnected(ProviderType.GOOGLE)) count++;
        if (oAuthProvider.isConnected(ProviderType.NAVER)) count++;
        return count;
    }

    public Map<ProviderType, OAuthProviderStatus> getOAuthProviderStatuses() {
        if (oAuthProvider == null) {
            return new HashMap<>();
        }
        return oAuthProvider.getOAuthProviderStatuses();
    }


    @JsonIgnore
    @OneToOne(mappedBy = "shelterMember")
    private Shelter shelter;


    // 1:N 관계 설정





    // Entity 메서드
    public Member(long id, String email, String nickname, String profileImageUrl, MemberRole role) {
        super(id, null, null);
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
    }

//    public void updateInfo(MemberUpdateRequest request) {
//        gender = request.getGender();
//        nickname = request.getNickname();
//        marketingAgree = request.isMarketingAgree();
//        birthDate = request.getBirthDate();
//        address = request.getAddress();
//        phoneNumber = request.getPhoneNumber();
//    }

    // 닉네임 업데이트 메서드
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 비밀번호 업데이트 메서드
    public void updatePassword(String password) {
        this.password = password;
    }

    // 전화번호 업데이트 메서드
    public void updatePhoneNum(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
