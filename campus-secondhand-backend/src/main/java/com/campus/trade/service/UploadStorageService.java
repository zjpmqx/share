package com.campus.trade.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UploadStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public boolean deleteLocalFileByUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        String path = url;
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                path = URI.create(url).getPath();
            }
        } catch (Exception ignored) {
            path = url;
        }

        if (path == null || !path.startsWith("/api/uploads/")) {
            return false;
        }

        String filename = path.substring("/api/uploads/".length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\")) {
            return false;
        }

        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = base.resolve(filename).normalize();
        if (!target.startsWith(base)) {
            return false;
        }

        try {
            return Files.deleteIfExists(target);
        } catch (Exception ignored) {
            return false;
        }
    }
}
