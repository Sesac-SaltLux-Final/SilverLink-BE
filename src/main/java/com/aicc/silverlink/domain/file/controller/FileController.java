package com.aicc.silverlink.domain.file.controller;

import com.aicc.silverlink.domain.file.dto.FileUploadResponse;
import com.aicc.silverlink.domain.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "파일", description = "파일 업로드/다운로드 API")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 단일 파일 업로드
     * POST /api/files/upload
     */
    @Operation(summary = "단일 파일 업로드", description = "파일을 AWS S3에 업로드합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "directory", defaultValue = "uploads") String directory) {
        FileUploadResponse response = fileService.uploadFile(file, directory);
        return ResponseEntity.ok(response);
    }

    /**
     * 다중 파일 업로드
     * POST /api/files/upload-multiple
     */
    @Operation(summary = "다중 파일 업로드", description = "여러 파일을 AWS S3에 업로드합니다.")
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileUploadResponse>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "directory", defaultValue = "uploads") String directory) {
        List<FileUploadResponse> responses = fileService.uploadFiles(files, directory);
        return ResponseEntity.ok(responses);
    }

    /**
     * 파일 삭제
     * DELETE /api/files/{filePath}
     */
    @Operation(summary = "파일 삭제", description = "S3에서 파일을 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<Void> deleteFile(@RequestParam("filePath") String filePath) {
        fileService.deleteFile(filePath);
        return ResponseEntity.ok().build();
    }

    /**
     * 파일 URL 조회
     * GET /api/files/url
     */
    @Operation(summary = "파일 URL 조회", description = "파일의 다운로드 URL을 반환합니다.")
    @GetMapping("/url")
    public ResponseEntity<String> getFileUrl(@RequestParam("filePath") String filePath) {
        String url = fileService.getPresignedUrl(filePath);
        return ResponseEntity.ok(url);
    }

    /**
     * 파일 다운로드
     * GET /api/files/download
     */
    @Operation(summary = "파일 다운로드", description = "파일을 다운로드합니다. (원본 파일명 유지)")
    @GetMapping("/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @RequestParam("filePath") String filePath,
            @RequestParam("originalFileName") String originalFileName) {
        try {
            org.springframework.core.io.Resource resource = fileService.loadFileAsResource(filePath);
            
            // 파일 확장자로 Content-Type 결정
            String contentType = determineContentType(originalFileName);
            
            // RFC 5987 형식으로 파일명 인코딩 (한글 파일명 지원)
            String encodedFileName = java.net.URLEncoder.encode(originalFileName, java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            
            // Content-Disposition 헤더 설정 (RFC 2231/5987 형식)
            String contentDisposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                originalFileName.replaceAll("[^\\x00-\\x7F]", "_"), // ASCII fallback
                encodedFileName
            );
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                    .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(org.springframework.http.HttpHeaders.PRAGMA, "no-cache")
                    .header(org.springframework.http.HttpHeaders.EXPIRES, "0")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 파일 확장자로 Content-Type 결정
     */
    private String determineContentType(String fileName) {
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }
        
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }
}
