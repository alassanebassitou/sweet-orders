package com.devcrafter.Patisserie.App.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Getter
    @Value("${app.cloudinary.folders.products}")
    private String productsFolder;

    @Getter
    @Value("${app.cloudinary.folders.categories}")
    private String categoriesFolder;

    /**
     * Upload an image to Cloudinary.
     * Returns the secure URL of the uploaded image.
     */
    public String uploadImage(MultipartFile file,
                              String folder) throws IOException {
        // Generate a unique public ID
        String publicId = folder + "/"
                + UUID.randomUUID().toString().substring(0, 8);

        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "public_id",       publicId,
                        "folder",          folder,
                        "resource_type",   "image",
                        // Auto-optimize quality
                        "quality",         "auto",
                        // Auto-format (WebP for modern browsers)
                        "fetch_format",    "auto",
                        // Resize to max 800px width
                        "transformation",  "w_800,c_limit"
                )
        );

        String url = (String) uploadResult.get("secure_url");
        log.info("Image uploaded to Cloudinary: {}", url);
        return url;
    }

    /**
     * Delete an image from Cloudinary by its URL.
     * Extracts the public_id from the URL automatically.
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            // Extract public_id from URL
            // URL format: https://res.cloudinary.com/{cloud}/image/upload/v{version}/{public_id}.{ext}
            String publicId = extractPublicId(imageUrl);
            if (publicId == null) return;

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.emptyMap()
            );
            log.info("Image deleted from Cloudinary: {}",
                    publicId);
        } catch (Exception e) {
            // Don't fail the main operation if deletion fails
            log.warn("Could not delete image from Cloudinary: {}",
                    e.getMessage());
        }
    }

    /**
     * Replace an existing image:
     * 1. Upload new image
     * 2. Delete old image from Cloudinary
     * Returns new URL.
     */
    public String replaceImage(MultipartFile newFile,
                               String oldUrl,
                               String folder) throws IOException {
        // Upload new image first
        String newUrl = uploadImage(newFile, folder);

        // Delete old image after successful upload
        if (oldUrl != null && !oldUrl.isBlank()) {
            deleteImage(oldUrl);
        }

        return newUrl;
    }


    private String extractPublicId(String url) {
        try {
            // Remove query params
            String cleanUrl = url.split("\\?")[0];
            // Find "/upload/" in the URL
            int uploadIndex = cleanUrl.indexOf("/upload/");
            if (uploadIndex == -1) return null;

            String afterUpload = cleanUrl
                    .substring(uploadIndex + 8);

            // Remove version prefix (v1234567890/)
            if (afterUpload.startsWith("v")
                    && afterUpload.contains("/")) {
                afterUpload = afterUpload
                        .substring(afterUpload.indexOf("/") + 1);
            }

            // Remove file extension
            int dotIndex = afterUpload.lastIndexOf(".");
            if (dotIndex != -1) {
                afterUpload = afterUpload.substring(0, dotIndex);
            }

            return afterUpload;
        } catch (Exception e) {
            log.warn("Could not extract public_id from: {}", url);
            return null;
        }
    }
}
