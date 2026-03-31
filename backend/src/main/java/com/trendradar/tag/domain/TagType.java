package com.trendradar.tag.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TagType {

    SURGE("급상승", "24시간 조회수 증가량 >= 500,000"),
    NEW_ENTRY("신규진입", "업로드 후 48시간 이내 TOP 50 진입"),
    HOT_COMMENT("화제성", "댓글수/조회수 비율 상위 10%"),
    HIGH_ENGAGE("고참여율", "좋아요수/조회수 비율 상위 10%"),
    LONG_RUN("롱런", "7일 이상 연속 TOP 50 유지"),
    GLOBAL("글로벌", "3개국 이상 동시 TOP 50 진입"),
    COMEBACK("역주행", "업로드 30일 이후 TOP 50 재진입");

    private final String label;
    private final String description;
}
