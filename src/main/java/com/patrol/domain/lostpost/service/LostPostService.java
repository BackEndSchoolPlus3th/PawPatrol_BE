package com.patrol.domain.lostpost.service;

import com.patrol.api.lostfound.dto.FindPostResponseDto;
import com.patrol.api.lostpost.dto.LostPostRequestDto;
import com.patrol.api.lostpost.dto.LostPostResponseDto;
import com.patrol.domain.lostfound.entity.FindPost;
import com.patrol.domain.lostfound.repository.FindPostRepository;
import com.patrol.domain.lostpost.entity.LostPost;
import com.patrol.domain.lostpost.repository.LostPostRepository;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class LostPostService {
    private final LostPostRepository lostPostRepository;
    private final FindPostRepository findPostRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public LostPostResponseDto createLostPost(LostPostRequestDto requestDto, MultipartFile image) {
        Member author = memberRepository.findById(requestDto.getAuthorId())
                .orElseThrow(() -> new RuntimeException("해당 ID의 회원을 찾을 수 없습니다.")); // ✅ Member 조회 추가

        LostPost lostPost = new LostPost(requestDto);
        lostPost.setAuthor(author); // ✅ author 설정

        lostPostRepository.save(lostPost);
        return new LostPostResponseDto(lostPost);
    }

    @Transactional
    public LostPostResponseDto updateLostPost(Long postId, LostPostRequestDto requestDto) {
        LostPost lostPost = lostPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        lostPost.update(requestDto);
        return new LostPostResponseDto(lostPost);
    }

    @Transactional
    public void deleteLostPost(Long postId) {
        lostPostRepository.deleteById(postId);
    }

    @Transactional(readOnly = true)
    public Page<LostPostResponseDto> getAllLostPosts(Pageable pageable) {
        return lostPostRepository.findAll(pageable).map(LostPostResponseDto::new);
    }

    @Transactional(readOnly = true)
    public LostPostResponseDto getLostPostById(Long postId) {
        LostPost lostPost = lostPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        return new LostPostResponseDto(lostPost);
    }

    // 특정 실종 신고글에 연결된 제보글 목록 조회
    @Transactional(readOnly = true)
    public Page<FindPostResponseDto> getFindPostsByLostId(Long lostId, Pageable pageable) {
        Page<FindPost> findPosts = findPostRepository.findByLostPost_Id(lostId, pageable);
        return findPosts.map(post -> new FindPostResponseDto(

                post.getAuthor().getNickname(),
                post.getTitle(),
                post.getContent(),
                post.getLatitude(),
                post.getLongitude(),
                post.getFindTime(),
                post.getTags(),
                post.getCreatedAt(),
                post.getModifiedAt(),
                post.getBirthDate(),
                post.getBreed(),
                post.getName(),
                post.getCharacteristics(),
                post.getSize(),
                post.getGender()
        ));
    }
}
