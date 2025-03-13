package com.patrol.domain.animal.enums;

import lombok.Getter;

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
