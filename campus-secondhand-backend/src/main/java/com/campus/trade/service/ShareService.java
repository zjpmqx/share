package com.campus.trade.service;

import com.campus.trade.dto.share.CreateShareCommentRequest;
import com.campus.trade.dto.share.CreateShareRequest;
import com.campus.trade.entity.ShareComment;
import com.campus.trade.entity.SharePost;
import com.campus.trade.mapper.ShareCommentMapper;
import com.campus.trade.mapper.SharePostMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShareService {

    public static final String MEDIA_TYPE_NONE = "NONE";
    public static final String MEDIA_TYPE_IMAGE = "IMAGE";
    public static final String MEDIA_TYPE_VIDEO = "VIDEO";
    public static final String MEDIA_TYPE_URL = "URL";
    public static final String MEDIA_TYPE_FILE = "FILE";

    private final SharePostMapper sharePostMapper;
    private final ShareCommentMapper shareCommentMapper;

    public ShareService(SharePostMapper sharePostMapper, ShareCommentMapper shareCommentMapper) {
        this.sharePostMapper = sharePostMapper;
        this.shareCommentMapper = shareCommentMapper;
    }

    public SharePost create(long userId, CreateShareRequest request) {
        SharePost post = new SharePost();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        String mediaType = request.getMediaType();
        if (mediaType == null || mediaType.isBlank()) {
            mediaType = MEDIA_TYPE_NONE;
        }
        post.setMediaType(mediaType);
        post.setMediaUrl(request.getMediaUrl());
        post.setLinkUrl(request.getLinkUrl());

        if (MEDIA_TYPE_URL.equals(mediaType)) {
            if (post.getLinkUrl() == null || post.getLinkUrl().isBlank()) {
                throw new IllegalArgumentException("linkUrl is required when mediaType=URL");
            }
        }
        if (MEDIA_TYPE_IMAGE.equals(mediaType) || MEDIA_TYPE_VIDEO.equals(mediaType)) {
            if (post.getMediaUrl() == null || post.getMediaUrl().isBlank()) {
                throw new IllegalArgumentException("mediaUrl is required when mediaType is IMAGE/VIDEO");
            }
        }
        if (MEDIA_TYPE_FILE.equals(mediaType)) {
            if (post.getMediaUrl() == null || post.getMediaUrl().isBlank()) {
                throw new IllegalArgumentException("mediaUrl is required when mediaType=FILE");
            }
        }

        sharePostMapper.insert(post);
        return sharePostMapper.findById(post.getId());
    }

    public List<SharePost> list(int page, int size) {
        int limit = Math.min(Math.max(size, 1), 50);
        int offset = Math.max(page, 0) * limit;
        return sharePostMapper.list(limit, offset);
    }

    public SharePost getById(long id) {
        SharePost post = sharePostMapper.findById(id);
        if (post == null) {
            throw new IllegalArgumentException("Share not found");
        }
        return post;
    }

    public ShareComment postComment(long userId, long shareId, CreateShareCommentRequest request) {
        SharePost post = sharePostMapper.findById(shareId);
        if (post == null) {
            throw new IllegalArgumentException("Share not found");
        }
        ShareComment comment = new ShareComment();
        comment.setShareId(shareId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        shareCommentMapper.insert(comment);
        return comment;
    }

    public List<ShareComment> listComments(long shareId) {
        return shareCommentMapper.listByShareId(shareId);
    }
}
