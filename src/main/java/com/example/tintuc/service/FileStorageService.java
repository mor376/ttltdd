package com.example.tintuc.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/** Lưu ảnh vào thư mục "uploads" (nằm cạnh pom.xml). */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final Path root = Paths.get("uploads").toAbsolutePath().normalize();

    public FileStorageService() throws IOException {
        Files.createDirectories(root);
    }

    /** Trả về thông báo lỗi, hoặc null nếu file hợp lệ. */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED.contains(ext)) {
            return "Chỉ chấp nhận ảnh jpg, jpeg, png, gif, webp";
        }
        return null;
    }

    /** Lưu file, trả về tên file mới. */
    public String save(MultipartFile file) throws IOException {
        String name = UUID.randomUUID() + "." + extensionOf(file.getOriginalFilename());
        Files.copy(file.getInputStream(), root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return name;
    }

    public void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) return;
        try {
            Path target = root.resolve(fileName).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
        }
    }

    private String extensionOf(String originalName) {
        if (originalName == null || !originalName.contains(".")) return "";
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
    }
}
