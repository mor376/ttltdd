package com.example.tintuc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws IOException {
        fileStorageService = new FileStorageService();
    }

    @Test
    void acceptsValidPngImage() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "thumbnailFile", "cover.png", "image/png", validPng());

        assertNull(fileStorageService.validate(file));
    }

    @Test
    void rejectsSpoofedImageContentAndMismatchedMediaType() {
        MockMultipartFile invalidImage = new MockMultipartFile(
                "thumbnailFile", "cover.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0});
        MockMultipartFile mismatchedType = new MockMultipartFile(
                "thumbnailFile", "cover.png", "image/jpeg", new byte[]{1, 2, 3});

        assertNotNull(fileStorageService.validate(invalidImage));
        assertNotNull(fileStorageService.validate(mismatchedType));
    }

    @Test
    void rejectsUnsupportedFilesAndOversizedImages() {
        MockMultipartFile svg = new MockMultipartFile(
                "thumbnailFile", "cover.svg", "image/svg+xml", "<svg/>".getBytes());
        MockMultipartFile oversized = new MockMultipartFile(
                "thumbnailFile", "cover.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertNotNull(fileStorageService.validate(svg));
        assertNotNull(fileStorageService.validate(oversized));
    }

    private byte[] validPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
