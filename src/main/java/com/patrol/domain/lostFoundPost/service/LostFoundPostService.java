package com.patrol.domain.lostFoundPost.service;

import com.patrol.api.comment.dto.CommentResponseDto;
import com.patrol.api.lostFoundPost.dto.LostFoundPostDetailResponseDto;
import com.patrol.api.lostFoundPost.dto.LostFoundPostRequestDto;
import com.patrol.api.lostFoundPost.dto.LostFoundPostResponseDto;
import com.patrol.api.member.auth.dto.MyPostsResponse;
import com.patrol.domain.ai.service.AiImageService;
import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animal.enums.AnimalType;
import com.patrol.domain.animal.repository.AnimalRepository;
import com.patrol.domain.comment.service.CommentService;
import com.patrol.domain.image.service.ImageHandlerService;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
import com.patrol.domain.lostFoundPost.repository.LostFoundPostRepository;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.image.repository.ImageRepository;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.global.error.ErrorCode;
import com.patrol.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LostFoundPostService {
    private final LostFoundPostRepository lostFoundPostRepository;
    private final AnimalRepository animalRepository;
    private final ImageRepository imageRepository;
    private final ImageHandlerService imageHandlerService;
    private final AiImageService aiImageService;
    private final CommentService commentService;
    private static final String FOLDER_PATH = "lostfoundpost/";

    @Transactional
    public LostFoundPostResponseDto createLostFoundPost(LostFoundPostRequestDto requestDto, Long petId, Member author, List<MultipartFile> images) {
        Animal pet = null;

        if (requestDto.getPetId() != null) {
            pet = animalRepository.findById(requestDto.getPetId())
                    .orElseThrow(() -> new IllegalArgumentException("해당하는 반려동물이 없습니다."));
            pet.markAsLost();
        }

        AnimalType animalType = requestDto.getAnimalType() != null
                ? AnimalType.valueOf(requestDto.getAnimalType())
                : null;

        LostFoundPost lostFoundPost = new LostFoundPost(requestDto, author, pet, animalType);
        lostFoundPostRepository.save(lostFoundPost);

        if (pet != null) {
            Image petImage = imageRepository.findByPath(pet.getImageUrl());
            if (petImage.getStatus() != PostStatus.SIGHTED) {
                updateImageWithLostFoundPost(petImage, lostFoundPost);
            }
        }

        getSavedImages(images, lostFoundPost);
        aiImageService.saveAiImages(images, lostFoundPost.getId(), lostFoundPost);
        return LostFoundPostResponseDto.from(lostFoundPost);
    }


    @Transactional
    public LostFoundPostResponseDto updateLostFoundPost(Long postId, LostFoundPostResponseDto requestDto, List<MultipartFile> images, Member author) {

        LostFoundPost lostFoundPost = lostFoundPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!lostFoundPost.getAuthor().equals(author)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (requestDto.getContent() != null) lostFoundPost.setContent(requestDto.getContent());
        if (requestDto.getLatitude() != null) lostFoundPost.setLatitude(requestDto.getLatitude());
        if (requestDto.getLongitude() != null) lostFoundPost.setLongitude(requestDto.getLongitude());
        if (requestDto.getLocation() != null) lostFoundPost.setLocation(requestDto.getLocation());
        if (requestDto.getFindTime() != null) lostFoundPost.setFindTime(requestDto.getFindTime());
        if (requestDto.getLostTime() != null) lostFoundPost.setLostTime(requestDto.getLostTime());
        if (requestDto.getStatus() != null) {
            PostStatus newStatus = PostStatus.fromString(requestDto.getStatus());
            lostFoundPost.setStatus(newStatus);

            List<Image> relatedImages = imageRepository.findAllByFoundId(postId);
            for (Image image : relatedImages) {
                image.updateStatus(newStatus);
                imageRepository.save(image);
            }
        }

        return getSavedImages(images, lostFoundPost);
    }

    @Transactional
    public void deleteLostFoundPost(Long postId, Member author) {

        LostFoundPost lostFoundPost = lostFoundPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!lostFoundPost.getAuthor().equals(author)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        List<Image> images = imageRepository.findAllByFoundId(postId);

        for (Image image : images) {
            if (image.getAnimalId() != null) {
                image.setFoundId(null);
                imageRepository.save(image);
            } else {
                imageHandlerService.deleteImage(image);
            }
        }
        lostFoundPostRepository.deleteById(postId);
    }



    @Transactional(readOnly = true)
    public Page<LostFoundPostResponseDto> getAllLostFoundPosts(Pageable pageable) {
        Page<LostFoundPost> findPosts = lostFoundPostRepository.findAll(pageable);
        return findPosts.map(LostFoundPostResponseDto::from);
    }

    @Transactional(readOnly = true)
    public LostFoundPostDetailResponseDto getLostFoundPostById(Long postId) {
        LostFoundPost lostFoundPost = lostFoundPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        List<CommentResponseDto> comments = commentService.getCommentsByLostFoundPost(postId);
        return LostFoundPostDetailResponseDto.from(lostFoundPost, comments);
    }


    @Transactional(readOnly = true)
    public List<LostFoundPostResponseDto> getLostFoundPostsWithinRadius(double latitude, double longitude, double radius) {
        List<LostFoundPost> lostFoundPosts = lostFoundPostRepository.findPostsWithinRadius(latitude, longitude, radius);
        return lostFoundPosts.stream()
                .map(LostFoundPostResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LostFoundPostResponseDto> getPostsByStatus(PostStatus postStatus, Pageable pageable) {
        Page<LostFoundPost> posts = lostFoundPostRepository.findByStatus(postStatus, pageable);
        return posts.map(LostFoundPostResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Page<MyPostsResponse> myPosts(Member member, Pageable pageable) {
        Page<LostFoundPost> postsPage = lostFoundPostRepository.findByAuthorId(member.getId(), pageable);

        return postsPage.map(post -> new MyPostsResponse(
                post.getId(),
                post.getContent(),
                post.getStatus(),
                post.getFindTime(),
                post.getLostTime(),
                post.getCreatedAt().toString()
        ));
    }

    @Transactional(readOnly = true)
    public Page<MyPostsResponse> myReportPosts(Member member, Pageable pageable) {
        Page<LostFoundPost> reportPosts = lostFoundPostRepository.findByAuthorIdAndStatusIn(
                member.getId(),
                List.of(PostStatus.FINDING, PostStatus.FOUND),
                pageable
        );

        return reportPosts.map(post -> new MyPostsResponse(
                post.getId(),
                post.getContent(),
                post.getStatus(),
                post.getFindTime(),
                post.getLostTime(),
                post.getCreatedAt().toString()
        ));
    }

    @Transactional(readOnly = true)
    public Page<MyPostsResponse> myWitnessPosts(Member member, Pageable pageable) {
        Page<LostFoundPost> witnessPosts = lostFoundPostRepository.findByAuthorIdAndStatusIn(
                member.getId(),
                List.of(PostStatus.SHELTER, PostStatus.FOSTERING, PostStatus.SIGHTED),
                pageable
        );

        return witnessPosts.map(post -> new MyPostsResponse(
                post.getId(),
                post.getContent(),
                post.getStatus(),
                post.getFindTime(),
                post.getLostTime(),
                post.getCreatedAt().toString()
        ));
    }

    private void updateImageWithLostFoundPost(Image image, LostFoundPost lostFoundPost) {
        image.setFoundId(lostFoundPost.getId());
        image.setStatus(lostFoundPost.getStatus());
        image.setAnimalType(lostFoundPost.getAnimalType());
        imageRepository.save(image);
    }

    @NotNull
    private LostFoundPostResponseDto getSavedImages(List<MultipartFile> images, LostFoundPost lostFoundPost) {
        if (images != null && !images.isEmpty()) {
            List<Image> savedImages = imageHandlerService.uploadAndRegisterImages(
                    images,
                    FOLDER_PATH,
                    null,
                    lostFoundPost.getId(),
                    lostFoundPost.getStatus(),
                    lostFoundPost.getAnimalType()
            );
            for (Image image : savedImages) {
                lostFoundPost.addImage(image);
            }
        }
        return LostFoundPostResponseDto.from(lostFoundPost);
    }

    @Transactional(readOnly = true)
    public Page<LostFoundPostResponseDto> getRewardPosts(PostStatus postStatus, Pageable pageable) {
        Page<LostFoundPost> posts = lostFoundPostRepository.findByStatusAndRewardNotNull(postStatus, pageable);
        return posts.map(LostFoundPostResponseDto::from);
    }
}
