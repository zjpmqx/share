package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private static final Set<String> ALLOWED_EXT = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp",
            ".mp4", ".webm", ".mov", ".m4v", ".avi",
            ".zip", ".rar", ".7z", ".tar", ".gz", ".tgz"
    );

    @Value("${app.upload.dir:uploads/public}")
    private String uploadDir;

    @Value("${app.upload.quarantine-dir:uploads/quarantine}")
    private String quarantineDir;

    @Value("${app.upload.max-bytes:10485760}")
    private long maxBytes;

    @Value("${app.upload.virus-scan.enabled:false}")
    private boolean virusScanEnabled;

    @Value("${app.upload.virus-scan.command:}")
    private String virusScanCommand;

    @Value("${app.upload.chunk-max-bytes:5242880}")
    private long chunkMaxBytes;

    private static final String CHUNK_META_NAME = "meta.properties";
    private static final String CHUNK_RESULT_NAME = "result.properties";
    private static final String CHUNK_LOCK_NAME = "merge.lock";

    // Cloudflare 之类的代理对长连接有超时（常见 100s）限制；大文件分片合并容易触发 524。
    // 这里对超过阈值的分片合并改为异步，前端轮询结果获取最终 url。
    private static final long ASYNC_COMPLETE_THRESHOLD_BYTES = 64L * 1024 * 1024;

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private static final Set<String> VIDEO_CONTENT_TYPES = Set.of(
            "video/mp4",
            "video/webm",
            "video/quicktime",
            "video/x-msvideo",
            "video/avi"
    );

    private static final Set<String> ARCHIVE_CONTENT_TYPES = Set.of(
            "application/zip",
            "application/x-zip-compressed",
            "application/x-7z-compressed",
            "application/x-7z",
            "application/7z",
            "application/x-7z-archive",
            "application/x-rar-compressed",
            "application/vnd.rar",
            "application/rar",
            "application/x-rar",
            "application/vnd.android.package-archive",
            "application/octet-stream",
            "application/gzip",
            "application/x-gzip",
            "application/x-tar"
    );

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        long size = file.getSize();
        if (size <= 0) {
            throw new IllegalArgumentException("File is empty");
        }
        if (maxBytes > 0 && size > maxBytes) {
            throw new IllegalArgumentException("File is too large");
        }

        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        if (!StringUtils.hasText(original)) {
            original = "file";
        }
        String ext = "";
        String lower = original.toLowerCase(Locale.ROOT);
        int idx = lower.lastIndexOf('.');
        if (idx >= 0) {
            ext = lower.substring(idx);
        }

        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!StringUtils.hasText(contentType)) {
            if (isImageExt(ext) || isVideoExt(ext)) {
                throw new IllegalArgumentException("Missing Content-Type");
            }
            contentType = "application/octet-stream";
        }
        if (isImageExt(ext)) {
            if (!IMAGE_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("Invalid Content-Type");
            }
        } else if (isVideoExt(ext)) {
            if (!VIDEO_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("Invalid Content-Type");
            }
        } else {
            if (!ARCHIVE_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("Invalid Content-Type");
            }
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        Path quarantineBase = Paths.get(quarantineDir).toAbsolutePath().normalize();
        Files.createDirectories(quarantineBase);
        Path quarantineFile = quarantineBase.resolve(filename + ".uploading").normalize();
        if (!quarantineFile.startsWith(quarantineBase)) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, quarantineFile, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            validateMagic(ext, quarantineFile);

            if (virusScanEnabled) {
                runVirusScan(quarantineFile);
            }
        } catch (Exception e) {
            try {
                Files.deleteIfExists(quarantineFile);
            } catch (Exception ignored) {
            }
            throw e;
        }

        Path publicBase = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(publicBase);
        Path target = publicBase.resolve(filename).normalize();
        if (!target.startsWith(publicBase)) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        Files.move(quarantineFile, target, StandardCopyOption.REPLACE_EXISTING);
        File dest = Objects.requireNonNull(target.toFile());
        if (!dest.exists()) {
            throw new IllegalStateException("Upload failed");
        }

        return ApiResponse.ok("/api/uploads/" + filename);
    }

    public static class ChunkInitRequest {
        private String filename;
        private Long size;
        private Long chunkSize;
        private String contentType;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public Long getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(Long chunkSize) {
            this.chunkSize = chunkSize;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }

    public static class ChunkInitResponse {
        private String uploadId;
        private int totalChunks;
        private long chunkSize;

        public ChunkInitResponse() {
        }

        public ChunkInitResponse(String uploadId, int totalChunks, long chunkSize) {
            this.uploadId = uploadId;
            this.totalChunks = totalChunks;
            this.chunkSize = chunkSize;
        }

        public String getUploadId() {
            return uploadId;
        }

        public void setUploadId(String uploadId) {
            this.uploadId = uploadId;
        }

        public int getTotalChunks() {
            return totalChunks;
        }

        public void setTotalChunks(int totalChunks) {
            this.totalChunks = totalChunks;
        }

        public long getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(long chunkSize) {
            this.chunkSize = chunkSize;
        }
    }

    @PostMapping("/upload/init")
    public ApiResponse<ChunkInitResponse> initChunkUpload(@RequestBody ChunkInitRequest request, HttpServletRequest httpRequest) throws Exception {
        long startNs = System.nanoTime();

        String original = Objects.toString(request == null ? null : request.getFilename(), "");
        long size = request == null || request.getSize() == null ? -1 : request.getSize();
        long chunkSize = request == null || request.getChunkSize() == null ? -1 : request.getChunkSize();
        String contentType = normalizeContentType(request == null ? null : request.getContentType());

        if (!StringUtils.hasText(original)) {
            throw new IllegalArgumentException("filename is required");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size is required");
        }
        if (maxBytes > 0 && size > maxBytes) {
            throw new IllegalArgumentException("File is too large");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize is required");
        }
        if (chunkMaxBytes > 0 && chunkSize > chunkMaxBytes) {
            throw new IllegalArgumentException("chunkSize is too large");
        }

        String lower = original.toLowerCase(Locale.ROOT);
        int idx = lower.lastIndexOf('.');
        String ext = idx >= 0 ? lower.substring(idx) : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        int totalChunks = (int) ((size + chunkSize - 1) / chunkSize);
        if (totalChunks <= 0) {
            throw new IllegalArgumentException("Invalid chunks");
        }

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        Path base = getChunkBase(uploadId);
        Files.createDirectories(base);

        Properties p = new Properties();
        p.setProperty("filename", original);
        p.setProperty("ext", ext);
        p.setProperty("size", String.valueOf(size));
        p.setProperty("chunkSize", String.valueOf(chunkSize));
        p.setProperty("totalChunks", String.valueOf(totalChunks));
        p.setProperty("contentType", contentType);

        try (OutputStream out = Files.newOutputStream(base.resolve(CHUNK_META_NAME), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            p.store(out, null);
        }

        long costMs = (System.nanoTime() - startNs) / 1_000_000;
        String remote = httpRequest == null ? "" : Objects.toString(httpRequest.getRemoteAddr(), "");
        log.info("upload_init uploadId={} remote={} filename={} size={} chunkSize={} totalChunks={} costMs={} contentType={} ",
                uploadId, remote, original, size, chunkSize, totalChunks, costMs, contentType);

        return ApiResponse.ok(new ChunkInitResponse(uploadId, totalChunks, chunkSize));
    }

    @PostMapping("/upload/chunk")
    public ApiResponse<String> uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("index") int index,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest
    ) throws Exception {
        long startNs = System.nanoTime();
        if (!StringUtils.hasText(uploadId)) {
            throw new IllegalArgumentException("uploadId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        long size = file.getSize();
        if (size <= 0) {
            throw new IllegalArgumentException("File is empty");
        }
        if (chunkMaxBytes > 0 && size > chunkMaxBytes) {
            throw new IllegalArgumentException("Chunk is too large");
        }

        Properties meta = readChunkMeta(uploadId);
        int totalChunks = Integer.parseInt(meta.getProperty("totalChunks", "0"));
        if (index < 0 || index >= totalChunks) {
            throw new IllegalArgumentException("Invalid chunk index");
        }

        Path base = getChunkBase(uploadId);
        Files.createDirectories(base);
        Path chunkPath = base.resolve(String.valueOf(index) + ".part").normalize();
        if (!chunkPath.startsWith(base)) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, chunkPath, StandardCopyOption.REPLACE_EXISTING);
        }

        long costMs = (System.nanoTime() - startNs) / 1_000_000;
        String remote = httpRequest == null ? "" : Objects.toString(httpRequest.getRemoteAddr(), "");
        log.info("upload_chunk uploadId={} index={} remote={} bytes={} costMs={}", uploadId, index, remote, size, costMs);

        return ApiResponse.ok("OK");
    }

    @GetMapping("/upload/result")
    public ResponseEntity<ApiResponse<String>> chunkUploadResult(@RequestParam("uploadId") String uploadId) {
        if (!StringUtils.hasText(uploadId)) {
            throw new IllegalArgumentException("uploadId is required");
        }

        Properties existingResult = readChunkResultIfExists(uploadId);
        if (existingResult != null) {
            String url = Objects.toString(existingResult.getProperty("url"), "");
            if (StringUtils.hasText(url)) {
                return ResponseEntity.ok(ApiResponse.ok(url));
            }
        }

        // 没有结果时返回 202，提示前端继续轮询；避免把“处理中”当作最终 url 写入业务数据。
        try {
            Path base = getChunkBase(uploadId);
            Path lock = base.resolve(CHUNK_LOCK_NAME).normalize();
            if (lock.startsWith(base) && Files.exists(lock)) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok("PROCESSING"));
            }
            // meta 存在说明上传记录还在；否则会抛 Upload not found
            readChunkMeta(uploadId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok("PROCESSING"));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception ignored) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok("PROCESSING"));
        }
    }

    @PostMapping("/upload/complete")
    public ResponseEntity<ApiResponse<String>> completeChunkUpload(@RequestParam("uploadId") String uploadId, HttpServletRequest httpRequest) throws Exception {
        long startNs = System.nanoTime();
        if (!StringUtils.hasText(uploadId)) {
            throw new IllegalArgumentException("uploadId is required");
        }

        Properties existingResult = readChunkResultIfExists(uploadId);
        if (existingResult != null) {
            String url = Objects.toString(existingResult.getProperty("url"), "");
            if (StringUtils.hasText(url)) {
                return ResponseEntity.ok(ApiResponse.ok(url));
            }
        }

        Properties meta = readChunkMeta(uploadId);
        String ext = Objects.toString(meta.getProperty("ext"), "").toLowerCase(Locale.ROOT);
        long totalSize = Long.parseLong(meta.getProperty("size", "-1"));
        long chunkSize = Long.parseLong(meta.getProperty("chunkSize", "-1"));
        int totalChunks = Integer.parseInt(meta.getProperty("totalChunks", "0"));
        String contentType = normalizeContentType(meta.getProperty("contentType"));

        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file type");
        }
        if (totalSize <= 0) {
            throw new IllegalArgumentException("Invalid file size");
        }
        if (maxBytes > 0 && totalSize > maxBytes) {
            throw new IllegalArgumentException("File is too large");
        }
        if (chunkSize <= 0 || totalChunks <= 0) {
            throw new IllegalArgumentException("Invalid chunks");
        }

        if (StringUtils.hasText(contentType)) {
            if (isImageExt(ext)) {
                if (!IMAGE_CONTENT_TYPES.contains(contentType)) {
                    throw new IllegalArgumentException("Invalid Content-Type");
                }
            } else if (isVideoExt(ext)) {
                if (!VIDEO_CONTENT_TYPES.contains(contentType)) {
                    throw new IllegalArgumentException("Invalid Content-Type");
                }
            } else {
                if (!ARCHIVE_CONTENT_TYPES.contains(contentType)) {
                    throw new IllegalArgumentException("Invalid Content-Type");
                }
            }
        }

        Path base = getChunkBase(uploadId);
        Files.createDirectories(base);
        Path lock = base.resolve(CHUNK_LOCK_NAME).normalize();
        if (!lock.startsWith(base)) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        // 大文件合并可能触发代理 524 超时：改为异步合并 + 前端轮询结果
        boolean async = totalSize >= ASYNC_COMPLETE_THRESHOLD_BYTES;

        // 尝试加锁，避免并发请求重复合并同一个 uploadId
        try {
            Files.writeString(lock, String.valueOf(System.currentTimeMillis()), StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok("PROCESSING"));
        }

        String remote = httpRequest == null ? "" : Objects.toString(httpRequest.getRemoteAddr(), "");

        if (async) {
            CompletableFuture.runAsync(() -> {
                try {
                    String url = mergeChunkUploadToPublic(uploadId, meta, ext, totalSize, totalChunks, contentType);
                    long costMs = (System.nanoTime() - startNs) / 1_000_000;
                    log.info("upload_complete_async uploadId={} remote={} totalSize={} totalChunks={} costMs={} url={} ",
                            uploadId, remote, totalSize, totalChunks, costMs, url);
                } catch (Exception e) {
                    log.error("upload_complete_async_failed uploadId={} remote={} totalSize={} totalChunks={} ",
                            uploadId, remote, totalSize, totalChunks, e);
                } finally {
                    deleteMergeLock(lock);
                }
            });
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok("PROCESSING"));
        }

        try {
            String url = mergeChunkUploadToPublic(uploadId, meta, ext, totalSize, totalChunks, contentType);
            long costMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("upload_complete uploadId={} remote={} totalSize={} totalChunks={} costMs={} url={} ",
                    uploadId, remote, totalSize, totalChunks, costMs, url);
            return ResponseEntity.ok(ApiResponse.ok(url));
        } finally {
            deleteMergeLock(lock);
        }
    }

    private String mergeChunkUploadToPublic(String uploadId, Properties meta, String ext, long totalSize, int totalChunks, String contentType) throws Exception {
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path quarantineBase = Paths.get(quarantineDir).toAbsolutePath().normalize();
        Files.createDirectories(quarantineBase);
        Path quarantineFile = quarantineBase.resolve(filename + ".uploading").normalize();
        if (!quarantineFile.startsWith(quarantineBase)) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        Path base = getChunkBase(uploadId);
        long written = 0;
        try (OutputStream out = Files.newOutputStream(quarantineFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunk = base.resolve(String.valueOf(i) + ".part").normalize();
                if (!chunk.startsWith(base) || !Files.exists(chunk)) {
                    throw new IllegalArgumentException("Missing chunk");
                }
                long chunkLen = Files.size(chunk);
                written += chunkLen;
                try (InputStream in = Files.newInputStream(chunk, StandardOpenOption.READ)) {
                    in.transferTo(out);
                }
            }
        } catch (Exception e) {
            try {
                Files.deleteIfExists(quarantineFile);
            } catch (Exception ignored) {
            }
            throw e;
        }

        if (written != totalSize) {
            try {
                Files.deleteIfExists(quarantineFile);
            } catch (Exception ignored) {
            }
            throw new IllegalArgumentException("Invalid file size");
        }

        try {
            validateMagic(ext, quarantineFile);

            if (virusScanEnabled) {
                runVirusScan(quarantineFile);
            }
        } catch (Exception e) {
            try {
                Files.deleteIfExists(quarantineFile);
            } catch (Exception ignored) {
            }
            throw e;
        }

        Path publicBase = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(publicBase);
        Path target = publicBase.resolve(filename).normalize();
        if (!target.startsWith(publicBase)) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        Files.move(quarantineFile, target, StandardCopyOption.REPLACE_EXISTING);
        File dest = Objects.requireNonNull(target.toFile());
        if (!dest.exists()) {
            throw new IllegalStateException("Upload failed");
        }

        String url = "/api/uploads/" + filename;
        writeChunkResult(uploadId, url, filename);
        cleanupChunkUploadParts(uploadId);
        return url;
    }

    private void deleteMergeLock(Path lock) {
        try {
            Files.deleteIfExists(lock);
        } catch (Exception ignored) {
        }
    }

    private static boolean isImageExt(String ext) {
        return ".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".gif".equals(ext) || ".webp".equals(ext);
    }

    private static String normalizeContentType(String contentType) {
        String s = Objects.toString(contentType, "");
        int idx = s.indexOf(';');
        if (idx >= 0) {
            s = s.substring(0, idx);
        }
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isVideoExt(String ext) {
        return ".mp4".equals(ext) || ".webm".equals(ext) || ".mov".equals(ext) || ".m4v".equals(ext) || ".avi".equals(ext);
    }

    private static boolean isArchiveExt(String ext) {
        return ".apk".equals(ext) || ".zip".equals(ext) || ".rar".equals(ext) || ".7z".equals(ext) || ".tar".equals(ext) || ".gz".equals(ext) || ".tgz".equals(ext);
    }

    private static byte[] readHeader(Path file, int len) throws IOException {
        byte[] buf = new byte[len];
        try (InputStream in = Files.newInputStream(file)) {
            int off = 0;
            while (off < len) {
                int r = in.read(buf, off, len - off);
                if (r < 0) {
                    break;
                }
                off += r;
            }
            if (off < len) {
                byte[] out = new byte[off];
                System.arraycopy(buf, 0, out, 0, off);
                return out;
            }
            return buf;
        }
    }

    private static void validateMagic(String ext, Path file) throws IOException {
        byte[] h = readHeader(file, 32);
        if (h.length < 12) {
            throw new IllegalArgumentException("Invalid file");
        }

        if (".jpg".equals(ext) || ".jpeg".equals(ext)) {
            if (!(h[0] == (byte) 0xFF && h[1] == (byte) 0xD8 && h[2] == (byte) 0xFF)) {
                throw new IllegalArgumentException("Invalid file");
            }
            return;
        }
        if (".png".equals(ext)) {
            byte[] sig = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            for (int i = 0; i < sig.length; i++) {
                if (h[i] != sig[i]) {
                    throw new IllegalArgumentException("Invalid file");
                }
            }
            return;
        }
        if (".gif".equals(ext)) {
            String s = new String(h, 0, Math.min(h.length, 6));
            if (!"GIF87a".equals(s) && !"GIF89a".equals(s)) {
                throw new IllegalArgumentException("Invalid file");
            }
            return;
        }
        if (".webp".equals(ext)) {
            if (!(h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P')) {
                throw new IllegalArgumentException("Invalid file");
            }
            return;
        }

        if (".webm".equals(ext)) {
            if (!(h[0] == 0x1A && (h[1] & 0xFF) == 0x45 && (h[2] & 0xFF) == 0xDF && (h[3] & 0xFF) == 0xA3)) {
                throw new IllegalArgumentException("Invalid file");
            }
            return;
        }
        if (".avi".equals(ext)) {
            if (!(h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'A' && h[9] == 'V' && h[10] == 'I' && h[11] == ' ')) {
                throw new IllegalArgumentException("Invalid file");
            }
            return;
        }
        if (".mp4".equals(ext) || ".mov".equals(ext) || ".m4v".equals(ext)) {
            if (!(h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p')) {
                throw new IllegalArgumentException("Invalid file");
            }
            return;
        }

        if (isArchiveExt(ext)) {
            if (".zip".equals(ext) || ".apk".equals(ext)) {
                if (h.length < 4) {
                    throw new IllegalArgumentException("Invalid file");
                }
                boolean ok = (h[0] == 'P' && h[1] == 'K' && (h[2] == 3 || h[2] == 5 || h[2] == 7) && (h[3] == 4 || h[3] == 6 || h[3] == 8));
                if (!ok) {
                    throw new IllegalArgumentException("Invalid file");
                }
                return;
            }
            if (".rar".equals(ext)) {
                if (!(h.length >= 7 && h[0] == 0x52 && h[1] == 0x61 && h[2] == 0x72 && h[3] == 0x21 && (h[4] & 0xFF) == 0x1A && h[5] == 0x07 && (h[6] == 0x00 || h[6] == 0x01))) {
                    throw new IllegalArgumentException("Invalid file");
                }
                return;
            }
            if (".7z".equals(ext)) {
                if (!(h.length >= 6 && h[0] == 0x37 && h[1] == 0x7A && (h[2] & 0xFF) == 0xBC && (h[3] & 0xFF) == 0xAF && h[4] == 0x27 && h[5] == 0x1C)) {
                    throw new IllegalArgumentException("Invalid file");
                }
                return;
            }
            if (".gz".equals(ext) || ".tgz".equals(ext)) {
                if (!(h.length >= 2 && (h[0] & 0xFF) == 0x1F && (h[1] & 0xFF) == 0x8B)) {
                    throw new IllegalArgumentException("Invalid file");
                }
                return;
            }
            if (".tar".equals(ext)) {
                byte[] header = readHeader(file, 512);
                if (header.length >= 262) {
                    boolean ustar = header[257] == 'u' && header[258] == 's' && header[259] == 't' && header[260] == 'a' && header[261] == 'r';
                    if (ustar) {
                        return;
                    }
                }
                return;
            }
        }

        throw new IllegalArgumentException("Unsupported file type");
    }

    private void runVirusScan(Path file) throws Exception {
        if (!StringUtils.hasText(virusScanCommand)) {
            throw new IllegalStateException("Virus scan is enabled but command is empty");
        }

        List<String> cmd = new ArrayList<>();
        for (String part : virusScanCommand.split("\\s+")) {
            if (!part.isBlank()) {
                cmd.add(part);
            }
        }
        cmd.add(file.toAbsolutePath().toString());

        Process p = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        int code = p.waitFor();
        if (code != 0) {
            try {
                Files.deleteIfExists(file);
            } catch (Exception ignored) {
            }
            throw new IllegalArgumentException("Virus scan failed");
        }
    }

    private Path getChunkBase(String uploadId) {
        Path base = Paths.get(quarantineDir).toAbsolutePath().normalize().resolve("chunk-uploads").resolve(uploadId).normalize();
        Path root = Paths.get(quarantineDir).toAbsolutePath().normalize().resolve("chunk-uploads").normalize();
        if (!base.startsWith(root)) {
            throw new IllegalArgumentException("Invalid upload path");
        }
        return base;
    }

    private Properties readChunkMeta(String uploadId) throws IOException {
        Path base = getChunkBase(uploadId);
        Path meta = base.resolve(CHUNK_META_NAME).normalize();
        if (!meta.startsWith(base) || !Files.exists(meta)) {
            throw new IllegalArgumentException("Upload not found");
        }

        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(meta, StandardOpenOption.READ)) {
            p.load(in);
        }
        return p;
    }

    private Properties readChunkResultIfExists(String uploadId) {
        try {
            Path base = getChunkBase(uploadId);
            Path meta = base.resolve(CHUNK_RESULT_NAME).normalize();
            if (!meta.startsWith(base) || !Files.exists(meta)) {
                return null;
            }

            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(meta, StandardOpenOption.READ)) {
                p.load(in);
            }
            return p;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeChunkResult(String uploadId, String url, String filename) {
        try {
            Path base = getChunkBase(uploadId);
            Files.createDirectories(base);
            Path outFile = base.resolve(CHUNK_RESULT_NAME).normalize();
            if (!outFile.startsWith(base)) {
                return;
            }

            Properties p = new Properties();
            p.setProperty("url", Objects.toString(url, ""));
            p.setProperty("filename", Objects.toString(filename, ""));
            try (OutputStream out = Files.newOutputStream(outFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                p.store(out, null);
            }
        } catch (Exception ignored) {
        }
    }

    private void cleanupChunkUploadParts(String uploadId) {
        try {
            Path base = getChunkBase(uploadId);
            if (!Files.exists(base)) {
                return;
            }
            Files.walk(base)
                    .forEach(p -> {
                        try {
                            String name = Objects.toString(p.getFileName(), "");
                            if (name.endsWith(".part")) {
                                Files.deleteIfExists(p);
                            }
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }
}
