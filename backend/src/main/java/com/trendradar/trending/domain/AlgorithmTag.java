package com.trendradar.trending.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "algorithm_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlgorithmTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false, length = 20)
    private String videoId;

    @Column(name = "tag_type", nullable = false, length = 50)
    private String tagType;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    @Builder
    private AlgorithmTag(String videoId, String tagType, OffsetDateTime calculatedAt) {
        this.videoId = videoId;
        this.tagType = tagType;
        this.calculatedAt = calculatedAt;
    }
}
