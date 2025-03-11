package com.patrol.global.storage;

import com.patrol.global.error.ErrorCode;
import com.patrol.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class FileStorageHandler {

    @Value("${ncp.storage.bucketname}")
    private String bucketName;
    private final StorageService storageService;

    // 파일 업로드 처리
    public FileUploadResult handleFileUpload(FileUploadRequest request) {
        try {
            String contentType = request.getFile().getContentType();
            List<String> allowedTypes = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/jpg");
            if (!allowedTypes.contains(contentType)) {
                throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
            }

            long maxSize = 5 * 1024 * 1024; // 5MB 제한
            if (request.getFile().getSize() > maxSize) {
                throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
            }

            String extension = getFileExtension(contentType);
            String filename = UUID.randomUUID().toString() + extension;

            HashMap<String, Object> options = new HashMap<>();
            options.put(StorageService.CONTENT_TYPE, contentType);
            options.put("contentLength", request.getFile().getSize());

            String filePath = request.getFolderPath() + filename;
            storageService.upload(filePath, request.getFile().getInputStream(), options);

            // ✅ 절대 URL 반환하도록 수정
            String fullUrl = "https://kr.object.ncloudstorage.com/" + bucketName + "/" + filePath;

            return FileUploadResult.builder()
                    .fileName(filename)
                    .fullPath(fullUrl)  // 절대경로 반환
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }


    // MIME type을 기반으로 확장자 추출
    private String getFileExtension(String contentType) {
        if (contentType.equals("image/jpeg") || contentType.equals("image/jpg")) {
            return ".jpg";
        } else if (contentType.equals("image/png")) {
            return ".png";
        } else if (contentType.equals("image/gif")) {
            return ".gif";
        }
        return "";
    }

    // 파일 삭제 처리
    public void handleFileDelete(String url) {
        try {
            if (url == null || url.isEmpty()) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }

            // 버킷 이름 이후의 경로를 파일 이름으로 사용
            String fileName = url.substring(url.indexOf(bucketName) + bucketName.length() + 1);

            // 파일 삭제
            storageService.delete(fileName);
        } catch (Exception e) {
            throw new RuntimeException("파일 삭제에 실패했습니다.", e);
        }
    }
}
