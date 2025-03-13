package com.patrol.domain.image.service;

import com.patrol.domain.animal.enums.AnimalType;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.image.repository.ImageRepository;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
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
@RequiredArgsConstructor
@Slf4j
public class ImageHandlerServiceImpl implements ImageHandlerService {
    private final ImageRepository imageRepository;
    private final FileStorageHandler fileStorageHandler;
    private final NcpObjectStorageService ncpObjectStorageService;

    @Value("${ncp.storage.endpoint}")
    private String endPoint;

    @Value("${ncp.storage.bucketname}")
    private String bucketName;

    @Override
    public String createImageUrl(String folderPath, String fileName) {
        return endPoint + "/" + bucketName + "/" + folderPath + fileName;
    }

    @Override
    @Transactional
    public Image registerImage (String imageUrl, Long animalId, Long foundId, PostStatus status, AnimalType animalType) {

        Image image = Image.builder()
                .path(imageUrl)
                .animalId(animalId)
                .foundId(foundId)
                .status(status)
                .animalType(animalType)
                .build();

        Image savedImage = imageRepository.save(image);

        return savedImage;
    }

    @Transactional
    @Override
    public List<Image> uploadAndRegisterImages(List<MultipartFile> files, String folderPath, Long animalId, Long foundId, PostStatus status, AnimalType animalType) {
        List<Image> savedImages = new ArrayList<>();
        List<String> uploadedPaths = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return savedImages;
        }

        try {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    FileUploadResult uploadResult = fileStorageHandler.handleFileUpload(
                            FileUploadRequest.builder()
                                    .folderPath(folderPath)
                                    .file(file)
                                    .build()
                    );

                    if (uploadResult != null) {
                        String fileName = uploadResult.getFileName();
                        uploadedPaths.add(fileName);

                        String imageUrl = createImageUrl(folderPath, fileName);
                        Image image = registerImage(imageUrl, animalId, foundId, status, animalType);
                        savedImages.add(image);
                    }
                }
            }
            return savedImages;
        } catch (Exception e) {
            for (String path : uploadedPaths) {
                ncpObjectStorageService.delete(path);
            }
            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }


    @Override
    public List<Image> uploadAndModifiedImages(List<MultipartFile> imageFiles, String folderPath, Long animalId) {
        List<Image> savedImages = new ArrayList<>();
        List<String> uploadedPaths = new ArrayList<>();

        try {
            for (MultipartFile file : imageFiles) {
                FileUploadResult uploadResult = fileStorageHandler.handleFileUpload(
                        FileUploadRequest.builder()
                                .folderPath(folderPath)
                                .file(file)
                                .build()
                );

                if (uploadResult != null) {
                    String fileName = uploadResult.getFileName();
                    uploadedPaths.add(fileName);

                    String imageUrl = createImageUrl(folderPath, fileName);

                    Image existingImage = imageRepository.findByAnimalId(animalId);
                    Long foundId = (existingImage != null) ? existingImage.getFoundId() : null;
                    PostStatus status = (existingImage != null) ? existingImage.getStatus() : null;
                    AnimalType animalType = (existingImage != null) ? existingImage.getAnimalType() : null;

                    Image image = registerImage(imageUrl, animalId, foundId, status, animalType);
                    savedImages.add(image);
                }
            }

            return savedImages;
        } catch (Exception e) {
            for (String path : uploadedPaths) {
                ncpObjectStorageService.delete(path);
            }

            throw new CustomException(ErrorCode.DATABASE_ERROR);
        }
    }


    @Override
    @Transactional
    public void deleteImage(Image image) {
        try {
            ncpObjectStorageService.delete(image.getPath());
            imageRepository.delete(image);
        } catch (Exception e) {
            log.error("이미지 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_DELETE_ERROR);
        }
    }

    @Override
    @Transactional
    public void deleteImageByPath(String path) {
        try {
            ncpObjectStorageService.delete(path);
            Image image = imageRepository.findByPath(path);
            if (image != null) {
                imageRepository.delete(image);
            }
        } catch (Exception e) {
            log.error("이미지 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_DELETE_ERROR);
        }
    }
}
