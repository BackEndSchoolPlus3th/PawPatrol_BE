package com.patrol.domain.image.service;

import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animal.repository.AnimalRepository;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.image.repository.ImageRepository;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.repository.LostFoundPostRepository;
import com.patrol.global.error.ErrorCode;
import com.patrol.global.exception.CustomException;
import com.patrol.global.storage.FileStorageHandler;
import com.patrol.global.storage.FileUploadRequest;
import com.patrol.global.storage.FileUploadResult;
import com.patrol.global.storage.NcpObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

    private final ImageRepository imageRepository;
    private final AnimalRepository animalRepository;
    private final FileStorageHandler fileStorageHandler;
    private final NcpObjectStorageService ncpObjectStorageService;
    private final LostFoundPostRepository lostFoundPostRepository;

    @Value("${ncp.storage.endpoint}")
    private String endPoint;

    @Value("${ncp.storage.bucketname}")
    private String bucketName;

    private static final String ANIMAL_FOLDER_PATH = "protection/";
    private static final String LOSTFOUND_FOLDER_PATH = "lostfoundpost/";

    @Transactional
    public List<Image> uploadAnimalImages(List<MultipartFile> images, Long animalId) {
        return uploadImages(images, ANIMAL_FOLDER_PATH, animalId, null);
    }

    @Transactional
    public Image saveImage(Image image) {
        log.info("이미지 저장: Path={}", image.getPath());
        Image savedImage = imageRepository.save(image);
        log.info("이미지 저장 완료: ID={}, Path={}", savedImage.getId(), savedImage.getPath());
        return savedImage;
    }

    public String uploadImageAndGetUrl(MultipartFile image, String folderPath) {
        try {
            FileUploadResult uploadResult = fileStorageHandler.handleFileUpload(
                    FileUploadRequest.builder()
                            .folderPath(folderPath)
                            .file(image)
                            .build()
            );

            if (uploadResult != null) {
                String imageUrl = endPoint + "/" + bucketName + "/" + folderPath + uploadResult.getFileName();
                return imageUrl;
            }
            return null;
        } catch (Exception e) {
            log.error("이미지 업로드 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    @Transactional
    public List<Image> uploadImages(List<MultipartFile> images, String folderPath, Long animalId, Long foundId) {

        List<String> uploadedPaths = new ArrayList<>();
        List<Image> uploadedImages = new ArrayList<>();

        try {
            for (MultipartFile image : images) {
                FileUploadResult uploadResult = fileStorageHandler.handleFileUpload(
                        FileUploadRequest.builder()
                                .folderPath(folderPath)
                                .file(image)
                                .build()
                );

                if (uploadResult != null) {
                    String fileName = uploadResult.getFileName();
                    String fullPath = folderPath + fileName;
                    uploadedPaths.add(fullPath);

                    String imageUrl = endPoint + "/" + bucketName + "/" + fullPath;

                    Image imageEntity = Image.builder()
                            .path(imageUrl)
                            .animalId(animalId)
                            .foundId(foundId)
                            .build();

                    Image savedImage = imageRepository.save(imageEntity);
                    uploadedImages.add(savedImage);
                }
            }
            return uploadedImages;
        } catch (Exception e) {
            for (String path : uploadedPaths) {
                try {
                    ncpObjectStorageService.delete(path);
                    log.info("업로드된 이미지 롤백 완료: Path={}", path);
                } catch (Exception ex) {
                    log.error("업로드된 이미지 롤백 실패: Path={}, 에러={}", path, ex.getMessage());
                }
            }
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }

    @Transactional
    public void deleteImages(List<Image> images) {
        images.forEach(image -> {
            try {
                String fullPath = image.getPath();
                String key = extractKeyFromUrl(fullPath);
                if (key != null) {
                    ncpObjectStorageService.delete(key);
                    imageRepository.delete(image);
                } else {
                    log.error("이미지 경로 추출 실패: ID={}, Path={}", image.getId(), fullPath);
                }
            } catch (Exception e) {
                log.error("이미지 삭제 실패: ID={}, Path={}, 에러={}", image.getId(), image.getPath(), e.getMessage());
            }
        });
    }

    @Transactional
    public void deleteImage(String imageUrl, Long loginUserId) {
        log.error(imageUrl);
        Image image = imageRepository.findByPath(imageUrl);
        if (image == null) {
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
        }

        if (image.getAnimalId() != null) {
            Animal animal = animalRepository.findById(image.getAnimalId())
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

            if (!animal.getOwner().getId().equals(loginUserId)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
            }

        } else if (image.getFoundId() != null) {
            LostFoundPost post = lostFoundPostRepository.findById(image.getFoundId())
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

            if (!post.getAuthor().getId().equals(loginUserId)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
            }
        } else {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        try {
            String fullPath = image.getPath();
            String key = extractKeyFromUrl(fullPath);

            if (key != null) {
                ncpObjectStorageService.delete(key);
                imageRepository.delete(image);
                log.info("이미지 삭제 완료: ID={}, Path={}", image.getId(), fullPath);
            } else {
                log.error("이미지 경로 추출 실패: ID={}, Path={}", image.getId(), fullPath);
                throw new CustomException(ErrorCode.FILE_DELETE_ERROR);
            }
        } catch (Exception e) {
            log.error("이미지 삭제 실패: ID={}, Path={}, 에러={}", image.getId(), image.getPath(), e.getMessage());
            throw new CustomException(ErrorCode.FILE_DELETE_ERROR);
        }
    }


    private String extractKeyFromUrl(String url) {
        try {
            int bucketEndIndex = url.indexOf(bucketName) + bucketName.length() + 1;
            return url.substring(bucketEndIndex);
        } catch (Exception e) {
            log.error("URL에서 키 추출 실패: {}, 에러: {}", url, e.getMessage());
            return null;
        }
    }

    public List<Image> findAllByAnimalId(Long animalId) {
      return imageRepository.findAllByAnimalId(animalId);
    }

    @Transactional
    public Image connectAnimal(String imageUrl, Long animalId) {
        Image image = imageRepository.findByPath(imageUrl);
        if (image == null) {
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
        }
        image.setAnimalId(animalId);
        return image;
    }
}
