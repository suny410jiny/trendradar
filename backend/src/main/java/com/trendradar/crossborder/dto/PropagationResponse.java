package com.trendradar.crossborder.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class PropagationResponse {
    private final String keyword;
    private final List<PropagationStep> propagationPath;

    @Getter
    @Builder
    public static class PropagationStep {
        private final String countryCode;
        private final OffsetDateTime firstSeenAt;
    }
}
