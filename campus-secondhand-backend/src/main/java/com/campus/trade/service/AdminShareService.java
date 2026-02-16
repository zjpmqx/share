package com.campus.trade.service;

import com.campus.trade.entity.SharePost;
import com.campus.trade.mapper.ShareCommentMapper;
import com.campus.trade.mapper.SharePostMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AdminShareService {

    private static final Pattern COVER_PATTERN = Pattern.compile("\\[\\[cover:([^\\]]+)\\]\\]");

    private final SharePostMapper sharePostMapper;
    private final ShareCommentMapper shareCommentMapper;
    private final UploadStorageService uploadStorageService;

    public AdminShareService(SharePostMapper sharePostMapper,
                            ShareCommentMapper shareCommentMapper,
                            UploadStorageService uploadStorageService) {
        this.sharePostMapper = sharePostMapper;
        this.shareCommentMapper = shareCommentMapper;
        this.uploadStorageService = uploadStorageService;
    }

    @Transactional
    public void deleteShare(long shareId) {
        SharePost post = sharePostMapper.findById(shareId);
        if (post == null) {
            return;
        }

        String mediaUrl = post.getMediaUrl();
        String content = post.getContent();

        shareCommentMapper.deleteByShareId(shareId);
        sharePostMapper.deleteById(shareId);

        uploadStorageService.deleteLocalFileByUrl(mediaUrl);
        if (content != null && !content.isBlank()) {
            Matcher m = COVER_PATTERN.matcher(content);
            while (m.find()) {
                String url = m.group(1);
                if (url != null && !url.isBlank()) {
                    uploadStorageService.deleteLocalFileByUrl(url.trim());
                }
            }
        }
    }
}
