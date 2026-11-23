package com.example.tintuc.entity;

public enum ActivityType {
    ARTICLE_CREATED("Đã đăng bài viết", "article", "blue"),
    ARTICLE_UPDATED("Đã chỉnh sửa bài viết", "edit", "blue"),
    ARTICLE_DELETED("Đã xóa bài viết", "delete", "red"),
    CATEGORY_CREATED("Đã tạo chuyên mục", "category", "violet"),
    CATEGORY_UPDATED("Đã cập nhật chuyên mục", "edit", "violet"),
    CATEGORY_DELETED("Đã xóa chuyên mục", "delete", "red"),
    USER_CREATED("Đã tạo người dùng", "user", "green"),
    USER_UPDATED("Đã cập nhật người dùng", "user", "green"),
    ADMIN_PROFILE_UPDATED("Đã cập nhật thông tin quản trị", "user", "green"),
    USER_DELETED("Đã xóa người dùng", "delete", "red"),
    COMMENT_CREATED("Đã tạo bình luận", "comment", "orange"),
    COMMENT_UPDATED("Đã cập nhật bình luận", "comment", "orange"),
    COMMENT_DELETED("Đã xóa bình luận", "delete", "red");

    private final String label;
    private final String icon;
    private final String tone;

    ActivityType(String label, String icon, String tone) {
        this.label = label;
        this.icon = icon;
        this.tone = tone;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public String getTone() {
        return tone;
    }
}
