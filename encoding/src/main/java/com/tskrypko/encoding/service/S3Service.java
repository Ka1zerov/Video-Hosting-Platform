package com.tskrypko.encoding.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    public void downloadFile(String s3Key, String localPath) throws IOException {
        logger.info("Downloading file from S3: {} -> {}", s3Key, localPath);
        
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, s3Key);
            S3Object s3Object = amazonS3.getObject(getObjectRequest);
            
            try (InputStream inputStream = s3Object.getObjectContent();
                 FileOutputStream outputStream = new FileOutputStream(localPath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            logger.info("File downloaded successfully: {}", localPath);
            
        } catch (Exception e) {
            logger.error("Error downloading file from S3: {}", e.getMessage(), e);
            throw new IOException("Failed to download file from S3", e);
        }
    }

    public void uploadFile(String localPath, String s3Key) throws IOException {
        logger.info("Uploading file to S3: {} -> {}", localPath, s3Key);
        
        try {
            File file = new File(localPath);
            if (!file.exists()) {
                throw new IOException("Local file does not exist: " + localPath);
            }

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            
            // Set content type based on file extension
            String contentType = getContentType(s3Key);
            metadata.setContentType(contentType);

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, s3Key, file);
            putObjectRequest.setMetadata(metadata);

            amazonS3.putObject(putObjectRequest);
            
            logger.info("File uploaded successfully to S3: {}", s3Key);
            
        } catch (Exception e) {
            logger.error("Error uploading file to S3: {}", e.getMessage(), e);
            throw new IOException("Failed to upload file to S3", e);
        }
    }

    public void deleteFile(String s3Key) {
        try {
            amazonS3.deleteObject(bucketName, s3Key);
            logger.info("File deleted from S3: {}", s3Key);
        } catch (Exception e) {
            logger.error("Error deleting file from S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    private String getContentType(String s3Key) {
        String fileName = s3Key.toLowerCase();
        
        if (fileName.endsWith(".m3u8")) {
            return "application/vnd.apple.mpegurl";
        } else if (fileName.endsWith(".ts")) {
            return "video/mp2t";
        } else if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else {
            return "application/octet-stream";
        }
    }
} 