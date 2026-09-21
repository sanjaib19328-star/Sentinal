package com.sentinel.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessingService.class);

    public static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/webp",
        "image/gif",
        "image/bmp"
    );

    public record ImageMetadata(
        boolean hasImage,
        String fileName,
        String contentType,
        long sizeBytes,
        int width,
        int height
    ) {}

    /**
     * Validates image MIME type, Base64 payload, and byte size.
     * Throws IllegalArgumentException on validation failure.
     */
    public byte[] validateAndDecodeImage(String fileBase64, String fileContentType) {
        if (fileBase64 == null || fileBase64.isBlank()) {
            throw new IllegalArgumentException("Image data is missing or empty.");
        }

        // Normalize and validate MIME type
        String normalizedMime = normalizeMimeType(fileContentType);
        if (!ALLOWED_MIME_TYPES.contains(normalizedMime)) {
            throw new IllegalArgumentException(
                "Unsupported image type '" + fileContentType + "'. Allowed types: PNG, JPEG, WEBP, GIF, BMP."
            );
        }

        // Clean any data URI header prefix if present (e.g. data:image/png;base64,...)
        String cleanBase64 = fileBase64;
        int commaIndex = cleanBase64.indexOf(',');
        if (commaIndex != -1 && cleanBase64.substring(0, commaIndex).contains(";base64")) {
            cleanBase64 = cleanBase64.substring(commaIndex + 1);
        }
        cleanBase64 = cleanBase64.trim().replaceAll("\\s+", "");

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(cleanBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed or invalid Base64 image payload: " + e.getMessage());
        }

        if (imageBytes.length == 0) {
            throw new IllegalArgumentException("Image payload is empty after decoding.");
        }

        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                "Image size (" + (imageBytes.length / (1024 * 1024)) + "MB) exceeds the maximum allowed limit of 10MB."
            );
        }

        return imageBytes;
    }

    /**
     * Inspects the decoded image to extract dimensions, format, and size.
     */
    public ImageMetadata inspectImage(String rawFileName, String fileContentType, byte[] imageBytes) {
        String sanitizedName = sanitizeFileName(rawFileName);
        String mime = normalizeMimeType(fileContentType);

        int width = 0;
        int height = 0;
        try {
            BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bimg != null) {
                width = bimg.getWidth();
                height = bimg.getHeight();
            }
        } catch (Exception e) {
            log.warn("Could not parse image dimensions for '{}': {}", sanitizedName, e.getMessage());
        }

        return new ImageMetadata(
            true,
            sanitizedName,
            mime,
            imageBytes.length,
            width,
            height
        );
    }

    /**
     * Executes an operation with a unique, auto-deleted temporary file containing the user's uploaded image bytes.
     * The temp file is guaranteed to be deleted upon completion in a finally block.
     */
    public <T> T withTempImageFile(byte[] imageBytes, String fileContentType, TempFileConsumer<T> consumer) throws Exception {
        String ext = extensionForMime(normalizeMimeType(fileContentType));
        Path tempFile = Files.createTempFile("sentinel_vision_" + UUID.randomUUID() + "_", ext);
        try {
            Files.write(tempFile, imageBytes);
            return consumer.accept(tempFile);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("Failed to delete temp image file {}: {}", tempFile, e.getMessage());
            }
        }
    }

    @FunctionalInterface
    public interface TempFileConsumer<T> {
        T accept(Path tempPath) throws Exception;
    }

    public static String normalizeMimeType(String mime) {
        if (mime == null || mime.isBlank()) {
            return "image/png";
        }
        String m = mime.trim().toLowerCase(Locale.ROOT);
        if (m.equals("image/jpg")) {
            return "image/jpeg";
        }
        return m;
    }

    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "uploaded_image_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
        }
        // Extract only the file name part, discarding any path traversal characters
        String clean = Path.of(fileName).getFileName().toString();
        // Remove any non-alphanumeric characters except dot, dash, underscore
        clean = clean.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (clean.isBlank()) {
            return "uploaded_image_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
        }
        return clean;
    }

    private String extensionForMime(String mime) {
        return switch (mime) {
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/bmp" -> ".bmp";
            default -> ".png";
        };
    }
}
