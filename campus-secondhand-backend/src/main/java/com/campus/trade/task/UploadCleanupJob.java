package com.campus.trade.task;

import com.campus.trade.mapper.ItemImageMapper;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.ShareCommentMapper;
import com.campus.trade.mapper.SharePostMapper;
import com.campus.trade.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class UploadCleanupJob {

    private static final Pattern COMMENT_IMG_PATTERN = Pattern.compile("\\[\\[img:([^\\]]+)\\]\\]");
    private static final Pattern SHARE_COVER_PATTERN = Pattern.compile("\\[\\[cover:([^\\]]+)\\]\\]");

    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final SharePostMapper sharePostMapper;
    private final ShareCommentMapper shareCommentMapper;
    private final UserMapper userMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.quarantine-dir:uploads/quarantine}")
    private String quarantineDir;

    @Value("${app.cleanup.cron:0 */10 * * * *}")
    private String cron;

    @Value("${app.cleanup.chunk-ttl-seconds:21600}")
    private long chunkTtlSeconds;

    @Value("${app.cleanup.temp-ttl-seconds:21600}")
    private long tempTtlSeconds;

    @Value("${app.cleanup.orphan-min-age-seconds:3600}")
    private long orphanMinAgeSeconds;

    @Value("${app.cleanup.max-delete-per-run:200}")
    private int maxDeletePerRun;

    public UploadCleanupJob(ItemMapper itemMapper,
                            ItemImageMapper itemImageMapper,
                            SharePostMapper sharePostMapper,
                            ShareCommentMapper shareCommentMapper,
                            UserMapper userMapper) {
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
        this.sharePostMapper = sharePostMapper;
        this.shareCommentMapper = shareCommentMapper;
        this.userMapper = userMapper;
    }

    @Scheduled(cron = "${app.cleanup.cron:0 */10 * * * *}")
    public void run() {
        cleanupChunkUploads();
        cleanupTempUploadingFiles();
        cleanupOrphanPublicUploads();
    }

    private void cleanupChunkUploads() {
        long now = System.currentTimeMillis();
        long cutoff = now - Math.max(60, chunkTtlSeconds) * 1000L;

        Path quarantineBase = Paths.get(quarantineDir).toAbsolutePath().normalize();
        Path chunkRoot = quarantineBase.resolve("chunk-uploads").normalize();
        if (!chunkRoot.startsWith(quarantineBase)) {
            return;
        }
        if (!Files.exists(chunkRoot) || !Files.isDirectory(chunkRoot)) {
            return;
        }

        int deleted = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(chunkRoot)) {
            for (Path p : ds) {
                if (deleted >= Math.max(1, maxDeletePerRun)) {
                    break;
                }
                if (!Files.isDirectory(p)) {
                    continue;
                }
                FileTime t;
                try {
                    t = Files.getLastModifiedTime(p);
                } catch (Exception e) {
                    continue;
                }
                if (t.toMillis() >= cutoff) {
                    continue;
                }
                if (deleteRecursively(p, chunkRoot)) {
                    deleted += 1;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void cleanupTempUploadingFiles() {
        long now = System.currentTimeMillis();
        long cutoff = now - Math.max(60, tempTtlSeconds) * 1000L;

        Path quarantineBase = Paths.get(quarantineDir).toAbsolutePath().normalize();
        if (!Files.exists(quarantineBase) || !Files.isDirectory(quarantineBase)) {
            return;
        }

        int deleted = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(quarantineBase, "*.uploading")) {
            for (Path p : ds) {
                if (deleted >= Math.max(1, maxDeletePerRun)) {
                    break;
                }
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                FileTime t;
                try {
                    t = Files.getLastModifiedTime(p);
                } catch (Exception e) {
                    continue;
                }
                if (t.toMillis() < cutoff) {
                    try {
                        Files.deleteIfExists(p);
                        deleted += 1;
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void cleanupOrphanPublicUploads() {
        Path uploadBase = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadBase) || !Files.isDirectory(uploadBase)) {
            return;
        }

        Set<String> referenced = collectReferencedFilenames();

        long now = System.currentTimeMillis();
        long cutoff = now - Math.max(60, orphanMinAgeSeconds) * 1000L;

        int deleted = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(uploadBase)) {
            for (Path p : ds) {
                if (deleted >= Math.max(1, maxDeletePerRun)) {
                    break;
                }
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                String name = Objects.toString(p.getFileName(), "");
                if (!StringUtils.hasText(name) || name.startsWith(".")) {
                    continue;
                }
                if (referenced.contains(name)) {
                    continue;
                }

                FileTime t;
                try {
                    t = Files.getLastModifiedTime(p);
                } catch (Exception e) {
                    continue;
                }
                if (t.toMillis() >= cutoff) {
                    continue;
                }

                try {
                    Files.deleteIfExists(p);
                    deleted += 1;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private Set<String> collectReferencedFilenames() {
        Set<String> out = new HashSet<>();
        try {
            for (String url : itemMapper.listAllCoverUrls()) {
                String name = extractUploadFilename(url);
                if (name != null) out.add(name);
            }
        } catch (Exception ignored) {
        }
        try {
            for (String url : itemImageMapper.listAllUrls()) {
                String name = extractUploadFilename(url);
                if (name != null) out.add(name);
            }
        } catch (Exception ignored) {
        }
        try {
            for (String url : sharePostMapper.listAllMediaUrls()) {
                String name = extractUploadFilename(url);
                if (name != null) out.add(name);
            }
        } catch (Exception ignored) {
        }
        try {
            for (String c : sharePostMapper.listAllContents()) {
                if (!StringUtils.hasText(c)) {
                    continue;
                }
                Matcher m = SHARE_COVER_PATTERN.matcher(c);
                while (m.find()) {
                    String url = Objects.toString(m.group(1), "").trim();
                    String name = extractUploadFilename(url);
                    if (name != null) out.add(name);
                }
            }
        } catch (Exception ignored) {
        }
        try {
            for (String c : shareCommentMapper.listAllContents()) {
                if (!StringUtils.hasText(c)) {
                    continue;
                }
                Matcher m = COMMENT_IMG_PATTERN.matcher(c);
                while (m.find()) {
                    String url = Objects.toString(m.group(1), "").trim();
                    String name = extractUploadFilename(url);
                    if (name != null) out.add(name);
                }
            }
        } catch (Exception ignored) {
        }
        try {
            for (String url : userMapper.listAllAvatarUrls()) {
                String name = extractUploadFilename(url);
                if (name != null) out.add(name);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static String extractUploadFilename(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        String path = url;
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                path = URI.create(url).getPath();
            }
        } catch (Exception ignored) {
            path = url;
        }

        if (!StringUtils.hasText(path) || !path.startsWith("/api/uploads/")) {
            return null;
        }

        String filename = path.substring("/api/uploads/".length());
        if (!StringUtils.hasText(filename) || filename.contains("/") || filename.contains("\\")) {
            return null;
        }

        return filename;
    }

    private static boolean deleteRecursively(Path target, Path mustBeUnder) {
        try {
            Path normalized = target.toAbsolutePath().normalize();
            Path base = mustBeUnder.toAbsolutePath().normalize();
            if (!normalized.startsWith(base)) {
                return false;
            }

            try (Stream<Path> walk = Files.walk(normalized)) {
                walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (Exception ignored) {
                            }
                        });
            }
            return !Files.exists(normalized);
        } catch (Exception ignored) {
            return false;
        }
    }
}
