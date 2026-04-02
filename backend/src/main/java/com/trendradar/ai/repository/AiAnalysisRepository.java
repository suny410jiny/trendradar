package com.trendradar.ai.repository;

import com.trendradar.ai.domain.AiAnalysis;
import com.trendradar.ai.domain.AnalysisType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

    Optional<AiAnalysis> findTopByAnalysisTypeAndTargetIdAndExpiresAtAfterOrderByCreatedAtDesc(
            AnalysisType analysisType, String targetId, OffsetDateTime now);
}
