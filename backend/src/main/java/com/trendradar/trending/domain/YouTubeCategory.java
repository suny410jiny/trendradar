package com.trendradar.trending.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum YouTubeCategory {

    MUSIC(10, "음악"),
    SPORTS(17, "스포츠"),
    GAMING(20, "게임"),
    VLOG(22, "일반"),
    ENTERTAINMENT(24, "엔터테인먼트"),
    NEWS(25, "뉴스/정치"),
    BEAUTY(26, "뷰티/패션"),
    EDUCATION(27, "교육"),
    SCIENCE(28, "과학/기술");

    private final int id;
    private final String name;

    public static List<YouTubeCategory> all() {
        return Arrays.asList(values());
    }

    public static String nameOf(int id) {
        return Arrays.stream(values())
                .filter(c -> c.id == id)
                .map(YouTubeCategory::getName)
                .findFirst()
                .orElse("기타");
    }
}
