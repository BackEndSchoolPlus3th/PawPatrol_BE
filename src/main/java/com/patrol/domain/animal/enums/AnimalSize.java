package com.patrol.domain.animal.enums;

import lombok.Getter;

/**
 * packageName    : com.patrol.domain.animal.enums
 * fileName       : AnimalSize
 * author         : sungjun
 * date           : 2025-02-24
 * description    : 자동 주석 생성
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-02-24        kyd54       최초 생성
 */
@Getter
public enum AnimalSize {
    SMALL("소형"),
    MEDIUM("중형"),
    LARGE("대형");

    private final String description;

    AnimalSize(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
