package com.trendradar.keyword.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "keyword_trends")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KeywordTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String keyword;

    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode;

    @Column(name = "video_count")
    private Integer videoCount;

    @Column(name = "total_views")
    private Long totalViews;

    @Column(name = "avg_engagement")
    private Double avgEngagement;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 10)
    private PeriodType periodType;

    @Column(name = "period_start", nullable = false)
    private OffsetDateTime periodStart;

    @Builder
    private KeywordTrend(String keyword, String countryCode, Integer videoCount,
                         Long totalViews, Double avgEngagement,
                         PeriodType periodType, OffsetDateTime periodStart) {
        this.keyword = keyword;
        this.countryCode = countryCode;
        this.videoCount = videoCount;
        this.totalViews = totalViews;
        this.avgEngagement = avgEngagement;
        this.periodType = periodType;
        this.periodStart = periodStart;
    }

    public void updateStats(Integer videoCount, Long totalViews, Double avgEngagement) {
        this.videoCount = videoCount;
        this.totalViews = totalViews;
        this.avgEngagement = avgEngagement;
    }
}
