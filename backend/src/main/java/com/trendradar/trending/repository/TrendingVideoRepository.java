package com.trendradar.trending.repository;

import com.trendradar.trending.domain.TrendingVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface TrendingVideoRepository extends JpaRepository<TrendingVideo, Long> {

    List<TrendingVideo> findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(
            String countryCode, OffsetDateTime from, OffsetDateTime to);

    List<TrendingVideo> findByVideoIdIn(List<String> videoIds);

    boolean existsByVideoIdAndCollectedAtBetween(
            String videoId, OffsetDateTime from, OffsetDateTime to);

    // SURGE용: 24시간 전 수집 데이터 Batch 조회
    @Query("SELECT tv FROM TrendingVideo tv WHERE tv.videoId IN :videoIds " +
            "AND tv.collectedAt BETWEEN :from AND :to")
    List<TrendingVideo> findByVideoIdInAndCollectedAtBetween(
            @Param("videoIds") List<String> videoIds,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    // LONG_RUN용: 7일간 서로 다른 날짜에 수집된 videoId 목록 (Batch)
    @Query(value = "SELECT tv.video_id FROM trending_videos tv " +
            "WHERE tv.video_id IN :videoIds AND tv.collected_at > :since " +
            "GROUP BY tv.video_id " +
            "HAVING COUNT(DISTINCT DATE_TRUNC('day', tv.collected_at)) >= 7",
            nativeQuery = true)
    List<String> findLongRunVideoIds(
            @Param("videoIds") List<String> videoIds,
            @Param("since") OffsetDateTime since);

    // GLOBAL용: 3개국 이상 동시 트렌딩 videoId 목록 (Batch)
    @Query(value = "SELECT tv.video_id FROM trending_videos tv " +
            "WHERE tv.video_id IN :videoIds AND tv.collected_at BETWEEN :from AND :to " +
            "GROUP BY tv.video_id " +
            "HAVING COUNT(DISTINCT tv.country_code) >= 3",
            nativeQuery = true)
    List<String> findGlobalVideoIds(
            @Param("videoIds") List<String> videoIds,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    // 채널별 트렌딩 영상 조회 (시간 범위)
    @Query("SELECT tv FROM TrendingVideo tv WHERE tv.channelId IN :channelIds " +
            "AND tv.collectedAt BETWEEN :from AND :to")
    List<TrendingVideo> findByChannelIdInAndCollectedAtBetween(
            @Param("channelIds") List<String> channelIds,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    // 최근 수집에서 고유 채널 ID 목록 (나라별)
    @Query(value = "SELECT DISTINCT tv.channel_id FROM trending_videos tv " +
            "WHERE tv.country_code = :countryCode AND tv.collected_at BETWEEN :from AND :to " +
            "AND tv.channel_id IS NOT NULL",
            nativeQuery = true)
    List<String> findDistinctChannelIdsByCountryCode(
            @Param("countryCode") String countryCode,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
