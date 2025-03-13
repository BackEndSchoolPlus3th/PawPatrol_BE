package com.patrol.domain.lostFoundPost.entity;


public enum PostStatus {
    SHELTER("보호소"),
    FOSTERING("임보 중"),
    FOUND("주인 찾기 완료"),
    FINDING("실종"),
    SIGHTED("목격");

    private final String description;

    PostStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static PostStatus fromString(String status) {
        for (PostStatus ps : PostStatus.values()) {
            if (ps.name().equalsIgnoreCase(status)) {
                return ps;
            }
        }
        throw new IllegalArgumentException("Invalid status value: " + status);
    }
}
//sdfdsf