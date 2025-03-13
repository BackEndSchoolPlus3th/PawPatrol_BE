package com.patrol.domain.member.member.repository;



import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>  {
    Optional<Member> findByEmail(String email);
    List<Member> findAllByPhoneNumber(String phoneNumber);
    Optional<Member> findByApiKey(String apiKey);
    List<Member> findAllByStatus(MemberStatus status);

    @Query("SELECT m FROM Member m JOIN m.oAuthProvider o WHERE o.kakao.providerId = :providerId")
    Optional<Member> findByKakaoProviderId(@Param("providerId") String providerId);

    @Query("SELECT m FROM Member m JOIN m.oAuthProvider o WHERE o.google.providerId = :providerId")
    Optional<Member> findByGoogleProviderId(@Param("providerId") String providerId);

    @Query("SELECT m FROM Member m JOIN m.oAuthProvider o WHERE o.naver.providerId = :providerId")
    Optional<Member> findByNaverProviderId(@Param("providerId") String providerId);

    Member getMemberById (long id);
}
