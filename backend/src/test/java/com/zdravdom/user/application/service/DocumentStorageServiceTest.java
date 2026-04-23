package com.zdravdom.user.application.service;

import com.zdravdom.global.testing.TestReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentStorageServiceTest {

    @Mock private S3Client s3Client;

    private DocumentStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new DocumentStorageService(s3Client);
        TestReflectionUtil.setField(storageService, "bucketDocuments", "zdravdom-documents");
    }

    @Nested
    @DisplayName("uploadDocument()")
    class UploadDocument {

        @Test
        @DisplayName("uploads file to S3 with correct bucket and key prefix")
        void uploadsToS3() throws IOException {
            MockMultipartFile file = new MockMultipartFile(
                "file", "license.pdf", "application/pdf", "PDF content".getBytes());

            String s3Key = storageService.uploadDocument(file, 42L, "license");

            assertThat(s3Key).startsWith("providers/42/license/");
            assertThat(s3Key).endsWith(".pdf");

            ArgumentCaptor<PutObjectRequest> reqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(reqCaptor.capture(), any(RequestBody.class));

            PutObjectRequest captured = reqCaptor.getValue();
            assertThat(captured.bucket()).isEqualTo("zdravdom-documents");
            assertThat(captured.contentType()).isEqualTo("application/pdf");
            assertThat(captured.contentLength()).isEqualTo(file.getSize());
        }

        @Test
        @DisplayName("returns the S3 key after successful upload")
        void returnsS3Key() throws IOException {
            MockMultipartFile file = new MockMultipartFile(
                "file", "cert.png", "image/png", "PNG data".getBytes());

            String s3Key = storageService.uploadDocument(file, 1L, "certification");

            assertThat(s3Key).contains("providers/1/certification/");
            assertThat(s3Key).endsWith(".png");
        }
    }

    @Nested
    @DisplayName("getDocumentUrl()")
    class GetDocumentUrl {

        @Test
        @DisplayName("returns URL with bucket and key")
        void returnsFormattedUrl() {
            String url = storageService.getDocumentUrl("providers/42/license/file.pdf");

            assertThat(url).isEqualTo("https://s3.amazonaws.com/zdravdom-documents/providers/42/license/file.pdf");
        }
    }

    @Nested
    @DisplayName("deleteDocument()")
    class DeleteDocument {

        @Test
        @DisplayName("deletes document from correct bucket")
        void deletesFromS3() {
            storageService.deleteDocument("providers/42/license/file.pdf");

            ArgumentCaptor<DeleteObjectRequest> reqCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
            verify(s3Client).deleteObject(reqCaptor.capture());

            DeleteObjectRequest captured = reqCaptor.getValue();
            assertThat(captured.bucket()).isEqualTo("zdravdom-documents");
            assertThat(captured.key()).isEqualTo("providers/42/license/file.pdf");
        }
    }
}
