package com.els.javatheorytrainer.service;

import com.els.javatheorytrainer.entity.Question;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded files on disk and returns public URLs.
 *
 * Physical storage example:
 * uploads/questions/java-core/jvm-memory/uuid.jpg
 *
 * Public browser URL example:
 * /uploads/questions/java-core/jvm-memory/uuid.jpg
 *
 * Database stores only public URL, not absolute disk path.
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );

    private final Path uploadsRoot;

    public FileStorageService(@Value("${app.uploads.root}") String uploadsRoot) {
        this.uploadsRoot = Paths.get(uploadsRoot).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadsRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create uploads root directory: " + this.uploadsRoot, e);
        }
    }

    /**
     * Stores image for a question.
     *
     * Folder structure:
     * questions/{volume-slug}/{section-slug}/{uuid}.{ext}
     *
     * We do not force WebP conversion because Java ImageIO/Thumbnailator
     * may not have a WebP writer in the current runtime.
     */
    public String storeQuestionImage(Question question, MultipartFile file) {
        if (question == null || question.getId() == null) {
            throw new IllegalArgumentException("Question must be saved before uploading images");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String originalFileName = validateImageAndGetCleanFilename(file);
        String originalExtension = getExtension(originalFileName);

        String volumeSlug = question.getSection().getVolume().getSlug();
        String sectionSlug = question.getSection().getSlug();

        validateSlug(volumeSlug, "volume slug");
        validateSlug(sectionSlug, "section slug");

        String subfolder = "questions/" + volumeSlug + "/" + sectionSlug;
        Path targetDirectory = uploadsRoot.resolve(subfolder).normalize();

        try {
            Files.createDirectories(targetDirectory);

            String targetExtension = normalizeTargetExtension(originalExtension);
            String filename = UUID.randomUUID() + targetExtension;

            Path targetPath = targetDirectory.resolve(filename).normalize();

            if (!targetPath.startsWith(targetDirectory)) {
                throw new IllegalArgumentException("Invalid file path");
            }

            saveImage(file, originalExtension, targetPath);

            return "/uploads/" + subfolder + "/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store uploaded image", e);
        }
    }

    /**
     * Deletes image file by public URL.
     *
     * Example:
     * /uploads/questions/java-core/jvm-memory/uuid.jpg
     */
    public void deleteByPublicUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }

        if (!publicUrl.startsWith("/uploads/")) {
            return;
        }

        String relativePath = publicUrl.substring("/uploads/".length());
        Path filePath = uploadsRoot.resolve(relativePath).normalize();

        if (!filePath.startsWith(uploadsRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + publicUrl, e);
        }
    }

    /**
     * Saves image in a stable format.
     *
     * jpg/jpeg -> resize + compress to jpg
     * png      -> resize to png
     * webp     -> copy as-is because WebP writing is not guaranteed
     */
    private void saveImage(MultipartFile file, String originalExtension, Path targetPath) throws IOException {
        if (originalExtension.equals(".webp")) {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        if (originalExtension.equals(".jpg") || originalExtension.equals(".jpeg")) {
            Thumbnails.of(file.getInputStream())
                    .size(1920, 1080)
                    .outputFormat("jpg")
                    .outputQuality(0.82)
                    .toFile(targetPath.toFile());
            return;
        }

        if (originalExtension.equals(".png")) {
            Thumbnails.of(file.getInputStream())
                    .size(1920, 1080)
                    .outputFormat("png")
                    .toFile(targetPath.toFile());
            return;
        }

        throw new IllegalArgumentException("Unsupported image extension: " + originalExtension);
    }

    private String validateImageAndGetCleanFilename(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
        );

        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence: " + originalFileName);
        }

        String extension = getExtension(originalFileName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported image extension: " + extension);
        }

        return originalFileName;
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("File has no extension");
        }

        return originalFilename
                .substring(originalFilename.lastIndexOf("."))
                .toLowerCase();
    }

    /**
     * jpeg and jpg are stored as .jpg.
     */
    private String normalizeTargetExtension(String originalExtension) {
        if (originalExtension.equals(".jpeg")) {
            return ".jpg";
        }

        return originalExtension;
    }

    private void validateSlug(String slug, String fieldName) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is empty");
        }

        if (slug.contains("/") || slug.contains("\\") || slug.contains("..")) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + slug);
        }
    }
}