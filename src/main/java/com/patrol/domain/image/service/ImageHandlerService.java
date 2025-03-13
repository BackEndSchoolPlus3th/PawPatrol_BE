package com.patrol.domain.image.service;

import com.patrol.domain.animal.enums.AnimalType;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageHandlerService {
    String createImageUrl(String folderPath, String fileName);

    // 이미지 삭제
    void deleteImage(Image image);

    // 경로로 이미지 삭제
    void deleteImageByPath(String path);

    Image registerImage (String imageUrl, Long animalId, Long foundId, PostStatus status, AnimalType animalType);

    List<Image> uploadAndRegisterImages (List<MultipartFile> files, String folderPath, Long animalId, Long foundId, PostStatus status, AnimalType animalType);

    List<Image> uploadAndModifiedImages (List< MultipartFile> imageFile, String folderPath, Long animalId);

}
