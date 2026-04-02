package com.trendradar.ai.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 30)
    private AnalysisType analysisType;

    @Column(name = "target_id", nullable = false, length = 30)
    private String targetId;

    @Column(name = "country_code", length = 5)
    private String countryCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "model_used", length = 50)
    private String modelUsed;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    @Builder
    private AiAnalysis(AnalysisType analysisType, String targetId, String countryCode,
                       String content, String modelUsed, OffsetDateTime expiresAt) {
        this.analysisType = analysisType;
        this.targetId = targetId;
        this.countryCode = countryCode;
        this.content = content;
        this.modelUsed = modelUsed;
        this.expiresAt = expiresAt;
    }
}
