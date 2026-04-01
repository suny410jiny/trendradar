package com.trendradar.keyword.repository;

import com.trendradar.keyword.domain.KeywordTrend;
import com.trendradar.keyword.domain.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface KeywordTrendRepository extends JpaRepository<KeywordTrend, Long> {

    Optional<KeywordTrend> findByKeywordAndCountryCodeAndPeriodTypeAndPeriodStart(
            String keyword, String countryCode, PeriodType periodType, OffsetDateTime periodStart);

    List<KeywordTrend> findByCountryCodeAndPeriodTypeAndPeriodStartOrderByVideoCountDesc(
            String countryCode, PeriodType periodType, OffsetDateTime periodStart);

    List<KeywordTrend> findByKeywordAndPeriodTypeOrderByPeriodStartDesc(
            String keyword, PeriodType periodType);

    List<KeywordTrend> findByPeriodTypeAndPeriodStartAfter(
            PeriodType periodType, OffsetDateTime after);

    List<KeywordTrend> findByCountryCodeAndPeriodTypeAndPeriodStartAfter(
            String countryCode, PeriodType periodType, OffsetDateTime after);
}
