package com.trendradar.crossborder.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GlobalLocalResponse {
    private final List<KeywordInfo> globalKeywords;
    private final List<KeywordInfo> localKeywords;

    @Getter
    @Builder
    public static class KeywordInfo {
        private final String keyword;
        private final List<String> countries;
        private final int videoCount;
    }
}
