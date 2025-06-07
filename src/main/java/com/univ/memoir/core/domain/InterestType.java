package com.univ.memoir.core.domain;

import lombok.Getter;

@Getter
public enum InterestType {
    WORK("업무"),
    STUDY("공부"),
    NEWS("뉴스"),
    CONTENTS("콘텐츠"),
    SHOPPING("쇼핑");

    private final String categoryName;

    InterestType(String categoryName) {
        this.categoryName = categoryName;
    }

}
