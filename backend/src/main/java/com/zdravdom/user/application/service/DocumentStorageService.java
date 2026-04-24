package com.zdravdom.user.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * S3 document storage service for provider and patient documents.
 */
@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket.documents:zdravdom-documents}") // PRODUCTION: Bucket name must come from environment-specific config — never a hardcoded default
    private String bucketDocuments;

    public DocumentStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Upload a file to S3 under the documents bucket.
     *
     * @param file       the multipart file data
     * @param providerId owner provider ID (used for key prefix)
     * @param documentType type folder (e.g. "license", "certification")
     * @return the S3 key where the file is stored
     */
    public String uploadDocument(MultipartFile file, Long providerId, String documentType) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String s3Key = String.format("providers/%d/%s/%s%s",
            providerId, documentType, UUID.randomUUID(), extension);

        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucketDocuments)
            .key(s3Key)
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

        log.info("Uploaded document to s3://{}/{}", bucketDocuments, s3Key);
        return s3Key;
    }

    /**
     * Generate a pre-signed URL for downloading a document.
     */
    public String getDocumentUrl(String s3Key) {
        // PRODUCTION: Return pre-signed S3 URL (S3Presigner) or CloudFront signed URL — direct URL exposes bucket structure and is a security risk
        return String.format("https://s3.amazonaws.com/%s/%s", bucketDocuments, s3Key);
    }

    /**
     * Delete a document from S3.
     */
    public void deleteDocument(String s3Key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
            .bucket(bucketDocuments)
            .key(s3Key)
            .build();
        s3Client.deleteObject(deleteRequest);
        log.info("Deleted document s3://{}/{}", bucketDocuments, s3Key);
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
