package com.example.tintuc.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Lưu ảnh vào thư mục "uploads" (nằm cạnh pom.xml). */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;
    private static final int MAX_IMAGE_DIMENSION = 10_000;

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

        if (file.getSize() > MAX_FILE_SIZE) {
            return "Ảnh tải lên không được vượt quá 5MB";
        }

        String contentType = file.getContentType();
        if (contentType == null || !expectedContentType(ext).equals(
                contentType.toLowerCase(Locale.ROOT).split(";", 2)[0].trim())) {
            return "Kiểu nội dung không khớp với định dạng ảnh";
        }

        try {
            byte[] header;
            try (InputStream in = file.getInputStream()) {
                header = in.readNBytes(12);
            }
            if (!looksLikeImage(ext, header)) {
                return "Nội dung tệp không phải ảnh hợp lệ";
            }
            if (!"webp".equals(ext) && !isDecodableImage(file, ext)) {
                return "Nội dung tệp không phải ảnh hợp lệ";
            }
        } catch (IOException e) {
            return "Không đọc được tệp tải lên";
        }
        return null;
    }

    /** Lưu file, trả về tên file mới. */
    public String save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Tệp ảnh không được để trống");
        }
        String validationError = validate(file);
        if (validationError != null) {
            throw new IOException(validationError);
        }

        String name = UUID.randomUUID() + "." + extensionOf(file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
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
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String expectedContentType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "";
        };
    }

    private boolean looksLikeImage(String extension, byte[] header) {
        return switch (extension) {
            case "jpg", "jpeg" -> header.length >= 3 && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            case "png" -> header.length >= 8 && (header[0] & 0xFF) == 0x89
                    && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                    && header[4] == 13 && header[5] == 10 && header[6] == 26 && header[7] == 10;
            case "gif" -> header.length >= 6 && header[0] == 'G' && header[1] == 'I'
                    && header[2] == 'F' && header[3] == '8'
                    && (header[4] == '7' || header[4] == '9') && header[5] == 'a';
            case "webp" -> header.length >= 12 && header[0] == 'R' && header[1] == 'I'
                    && header[2] == 'F' && header[3] == 'F' && header[8] == 'W'
                    && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }

    private boolean isDecodableImage(MultipartFile file, String extension) throws IOException {
        String expectedFormat = switch (extension) {
            case "jpg", "jpeg" -> "jpeg";
            default -> extension;
        };
        try (InputStream input = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) return false;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageInput, true, true);
                    if (!expectedFormat.equalsIgnoreCase(reader.getFormatName())) continue;
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
                            || (long) width * height > MAX_IMAGE_PIXELS) {
                        return false;
                    }
                    return reader.read(0) != null;
                } finally {
                    reader.dispose();
                }
            }
        }
        return false;
    }
}
