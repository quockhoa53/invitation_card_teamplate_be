package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB Max
    private final Path uploadDir = Paths.get("uploads");

    public UploadController() {
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/file")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn tệp để tải lên"));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Dung lượng tệp vượt quá giới hạn cho phép (Tối đa 10MB)"));
        }

        try {
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            }

            // Security check: Only allow safe image & audio extensions
            if (!extension.matches("\\.(jpg|jpeg|png|webp|gif|mp3|wav|ogg|m4a)$")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Định dạng tệp không được hỗ trợ. Chỉ hỗ trợ tệp ảnh và âm thanh hợp lệ."));
            }

            String filename = System.currentTimeMillis() + "_" + RandomStringUtils.randomAlphanumeric(8) + extension;
            Path targetPath = uploadDir.resolve(filename).normalize();

            // Prevent path traversal
            if (!targetPath.startsWith(uploadDir)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Tên tệp không hợp lệ"));
            }

            Files.copy(file.getInputStream(), targetPath);

            Map<String, String> result = new HashMap<>();
            result.put("fileName", filename);
            result.put("url", "/uploads/" + filename);

            // Only encode dataUri if file is small (< 500KB) to avoid memory spikes
            if (file.getSize() <= 500 * 1024) {
                byte[] bytes = file.getBytes();
                String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
                String base64Data = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
                result.put("dataUri", base64Data);
            } else {
                result.put("dataUri", "");
            }

            return ResponseEntity.ok(ApiResponse.ok("Tải tệp lên thành công", result));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Không thể lưu tệp: " + e.getMessage()));
        }
    }
}
