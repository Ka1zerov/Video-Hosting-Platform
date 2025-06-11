package com.tskrypko.upload.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    @Value("${aws.s3.bucket.prefix:videos/}")
    private String keyPrefix;

    /**
     * Загружает файл в S3 и возвращает ключ объекта
     */
    public String uploadFile(MultipartFile file, String userId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueKey = generateUniqueKey(userId, fileExtension);

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.addUserMetadata("original-filename", originalFilename);
            metadata.addUserMetadata("user-id", userId);

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName, 
                    uniqueKey, 
                    inputStream, 
                    metadata
            );

            amazonS3.putObject(putObjectRequest);
            
            logger.info("File successfully uploaded to S3: bucket={}, key={}, size={}", 
                       bucketName, uniqueKey, file.getSize());

            return uniqueKey;
        } catch (Exception e) {
            logger.error("Error uploading file to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    /**
     * Удаляет файл из S3
     */
    public void deleteFile(String s3Key) {
        try {
            amazonS3.deleteObject(bucketName, s3Key);
            logger.info("File successfully deleted from S3: bucket={}, key={}", bucketName, s3Key);
        } catch (Exception e) {
            logger.error("Error deleting file from S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    /**
     * Проверяет существование файла в S3
     */
    public boolean fileExists(String s3Key) {
        try {
            return amazonS3.doesObjectExist(bucketName, s3Key);
        } catch (Exception e) {
            logger.error("Error checking file existence in S3: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Генерирует уникальный ключ для файла
     */
    private String generateUniqueKey(String userId, String fileExtension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString();
        return String.format("%s%s/%s_%s%s", keyPrefix, userId, timestamp, uuid, fileExtension);
    }

    /**
     * Извлекает расширение файла
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Возвращает URL файла в S3
     */
    public String getFileUrl(String s3Key) {
        return amazonS3.getUrl(bucketName, s3Key).toString();
    }
} 