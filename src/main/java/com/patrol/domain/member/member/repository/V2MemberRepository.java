package com.patrol.domain.member.member.repository;

import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * packageName    : com.patrol.domain.member.member.repository
 * fileName       : V2MemberRepository
 * author         : sungjun
 * date           : 2025-02-19
 * description    : 자동 주석 생성
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-02-19        kyd54       최초 생성
 */
public interface V2MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByApiKey(String apiKey);
    Boolean existsByEmail(String email);
    Page<Member> findByRoleNot(MemberRole role, Pageable pageable);
}
