package com.trendradar.trending.repository;

import com.trendradar.trending.domain.TrendingVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface TrendingVideoRepository extends JpaRepository<TrendingVideo, Long> {

    List<TrendingVideo> findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(
            String countryCode, OffsetDateTime from, OffsetDateTime to);

    List<TrendingVideo> findByVideoIdIn(List<String> videoIds);

    boolean existsByVideoIdAndCollectedAtBetween(
            String videoId, OffsetDateTime from, OffsetDateTime to);
}
