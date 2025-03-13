package com.patrol.domain.member.member.repository;

import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface V2MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByApiKey(String apiKey);
    Boolean existsByEmail(String email);
    Page<Member> findByRoleNot(MemberRole role, Pageable pageable);
}
