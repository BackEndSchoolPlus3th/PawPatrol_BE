package com.patrol.domain.member.member.service;

import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.MemberStatus;
import com.patrol.domain.member.member.repository.MemberRepository;
import com.patrol.global.exceptions.ErrorCodes;
import com.patrol.global.exceptions.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;


    public List<Member> findAll() {
        return memberRepository.findAll();
    }


    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }

    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new ServiceException(ErrorCodes.MEMBER_NOT_FOUND));
    }

    @Transactional
    public void banMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new ServiceException(ErrorCodes.MEMBER_NOT_FOUND));

        member.setStatus(MemberStatus.BANNED);
    }

    @Transactional
    public void deactivateMember(Long memberId) {
        memberRepository.findById(memberId)
            .orElseThrow(() -> new ServiceException(ErrorCodes.MEMBER_NOT_FOUND))
            .deactivate();
    }

    @Transactional
    public void restoreMember(Long memberId) {
        memberRepository.findById(memberId)
            .orElseThrow(() -> new ServiceException(ErrorCodes.MEMBER_NOT_FOUND))
            .restore();
    }

}
