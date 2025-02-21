package com.patrol.domain.lostfound.service;

import com.patrol.api.lostfound.dto.FindPostRequestDto;
import com.patrol.api.lostfound.dto.FindPostResponseDto;
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
public class FindPostService {
    private final FindPostRepository findPostRepository;
    private final LostPostRepository lostPostRepository;  // Inject LostPostRepository
    private final MemberRepository memberRepository;
    //private final S3Service s3Service;

    // 신고글 연계 제보 게시글 등록
    @Transactional
    public FindPostResponseDto createFindPost(FindPostRequestDto requestDto, Long lostPostId, MultipartFile image) {
        // 작성자(Member) 조회
        Member author = memberRepository.findById(requestDto.getAuthorId())
                .orElseThrow(() -> new RuntimeException("해당 ID의 회원을 찾을 수 없습니다."));

        // LostPost 객체를 lostPostId로 조회
        LostPost lostPost = null;
        if (lostPostId != null) {
            lostPost = lostPostRepository.findById(lostPostId)
                    .orElseThrow(() -> new RuntimeException("실종 게시글을 찾을 수 없습니다."));
        }

        // FindPost 객체 생성 시 lostPost를 전달
        FindPost findPost = new FindPost(requestDto, lostPost);
        findPost.setAuthor(author); // ✅ 작성자 설정 추가
        findPostRepository.save(findPost);

        // FindPostResponseDto 생성
        return new FindPostResponseDto(

                findPost.getAuthor().getNickname(),

                findPost.getTitle(),
                findPost.getContent(),
                findPost.getLatitude(),
                findPost.getLongitude(),
                findPost.getFindTime(),
                findPost.getTags(),
                findPost.getCreatedAt(),
                findPost.getModifiedAt(),
                findPost.getBirthDate(),        // 출생일
                findPost.getBreed(),           // 품종
                findPost.getName(),            // 이름
                findPost.getCharacteristics(), // 특징
                findPost.getSize(),            // 크기
                findPost.getGender()           // 성별
        );
    }

    // 신고글 연계 제보 게시글 수정
    @Transactional
    public FindPostResponseDto updateFindPost(Long postId, Long lostPostId, FindPostRequestDto requestDto) {
        FindPost findPost = findPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        findPost.setTitle(requestDto.getTitle());
        findPost.setContent(requestDto.getContent());
        findPost.setFindTime(requestDto.getFindTime());
        findPost.setTags(String.join("#", requestDto.getTags()));

        // LostPost 연계
        if (lostPostId != null) {
            LostPost lostPost = lostPostRepository.findById(lostPostId)
                    .orElseThrow(() -> new RuntimeException("실종 게시글을 찾을 수 없습니다."));
            findPost.setLostPost(lostPost);
        }

        //findPost.setModifiedAt(LocalDateTime.now());

        return new FindPostResponseDto(

                findPost.getAuthor().getNickname(),

                findPost.getTitle(),
                findPost.getContent(),
                findPost.getLatitude(),
                findPost.getLongitude(),
                findPost.getFindTime(),
                findPost.getTags(),
                findPost.getCreatedAt(),
                findPost.getModifiedAt(),
                findPost.getBirthDate(),        // 출생일
                findPost.getBreed(),           // 품종
                findPost.getName(),            // 이름
                findPost.getCharacteristics(), // 특징
                findPost.getSize(),            // 크기
                findPost.getGender()           // 성별
        );
    }

    // 신고글 연계 제보 게시글 삭제
    @Transactional
    public void deleteFindPost(Long postId) {
        findPostRepository.deleteById(postId);
    }



    // Create a new standalone find post
    @Transactional
    public FindPostResponseDto createStandaloneFindPost(FindPostRequestDto requestDto, MultipartFile image) {
        // 작성자(Member) 조회
        Member author = memberRepository.findById(requestDto.getAuthorId())
                .orElseThrow(() -> new RuntimeException("해당 ID의 회원을 찾을 수 없습니다."));

        // 연계 없는 제보 게시글 생성
        FindPost findPost = new FindPost(requestDto, null);  // 연계된 실종 게시글 ID 없이 생성
        findPost.setAuthor(author); // ✅ 작성자 설정 추가
        findPostRepository.save(findPost);

        return new FindPostResponseDto(

                findPost.getAuthor().getNickname(),
                findPost.getTitle(),
                findPost.getContent(),
                findPost.getLatitude(),
                findPost.getLongitude(),
                findPost.getFindTime(),
                findPost.getTags(),
                findPost.getCreatedAt(),
                findPost.getModifiedAt(),
                findPost.getBirthDate(),        // 출생일
                findPost.getBreed(),           // 품종
                findPost.getName(),            // 이름
                findPost.getCharacteristics(), // 특징
                findPost.getSize(),            // 크기
                findPost.getGender()           // 성별
        );
    }

    // 연계 없는 제보 게시글 수정
    @Transactional
    public FindPostResponseDto updateStandaloneFindPost(Long postId, FindPostRequestDto requestDto) {
        FindPost findPost = findPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 연계 없는 제보 게시글 업데이트
        findPost.setTitle(requestDto.getTitle());
        findPost.setContent(requestDto.getContent());
        findPost.setFindTime(requestDto.getFindTime());
        findPost.setTags(String.join("#", requestDto.getTags()));
        //findPost.setModifiedAt(LocalDateTime.now());

        // Save the updated post
        findPostRepository.save(findPost);

        return new FindPostResponseDto(

                findPost.getAuthor().getNickname(),
                findPost.getTitle(),
                findPost.getContent(),
                findPost.getLatitude(),
                findPost.getLongitude(),
                findPost.getFindTime(),
                findPost.getTags(),
                findPost.getCreatedAt(),
                findPost.getModifiedAt(),
                findPost.getBirthDate(),        // 출생일
                findPost.getBreed(),           // 품종
                findPost.getName(),            // 이름
                findPost.getCharacteristics(), // 특징
                findPost.getSize(),            // 크기
                findPost.getGender()           // 성별
        );
    }

    // Delete a standalone find post
    @Transactional
    public void deleteStandaloneFindPost(Long postId) {
        findPostRepository.deleteById(postId);
    }

    @Transactional(readOnly = true)
    public Page<FindPostResponseDto> getAllStandaloneFindPosts(Pageable pageable) {
        return findPostRepository.findByLostPostIsNull(pageable) // ✅ 수정됨
                .map(post -> new FindPostResponseDto(
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

    @Transactional(readOnly = true)
    public Page<FindPostResponseDto> getAllFindPosts(Pageable pageable) {
        return findPostRepository.findByLostPostIsNotNull(pageable) // ✅ 수정됨
                .map(post -> new FindPostResponseDto(
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
    @Transactional(readOnly = true)
    public FindPostResponseDto getStandaloneFindPostById(Long postId) {
        FindPost findPost = findPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (findPost.getLostPost() != null) { // 🚨 신고글과 연계된 게시글이라면 예외 처리
            throw new RuntimeException("이 게시글은 독립적인 제보 게시글이 아닙니다.");
        }

        return new FindPostResponseDto(
                findPost.getAuthor().getNickname(),
                findPost.getTitle(),
                findPost.getContent(),
                findPost.getLatitude(),
                findPost.getLongitude(),
                findPost.getFindTime(),
                findPost.getTags(),
                findPost.getCreatedAt(),
                findPost.getModifiedAt(),
                findPost.getBirthDate(),
                findPost.getBreed(),
                findPost.getName(),
                findPost.getCharacteristics(),
                findPost.getSize(),
                findPost.getGender()
        );
    }


    @Transactional(readOnly = true)
    public FindPostResponseDto getFindPostById(Long postId) {
        FindPost findPost = findPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (findPost.getLostPost() == null) { // 🚨 독립적인 제보글이라면 예외 처리
            throw new RuntimeException("이 게시글은 신고글과 연계되지 않은 독립적인 제보 게시글입니다.");
        }

        return new FindPostResponseDto(
                findPost.getAuthor().getNickname(),
                findPost.getTitle(),
                findPost.getContent(),
                findPost.getLatitude(),
                findPost.getLongitude(),
                findPost.getFindTime(),
                findPost.getTags(),
                findPost.getCreatedAt(),
                findPost.getModifiedAt(),
                findPost.getBirthDate(),
                findPost.getBreed(),
                findPost.getName(),
                findPost.getCharacteristics(),
                findPost.getSize(),
                findPost.getGender()
        );
    }



}


