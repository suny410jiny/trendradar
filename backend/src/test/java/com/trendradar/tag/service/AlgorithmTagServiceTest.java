package com.trendradar.tag.service;

import com.trendradar.tag.domain.TagType;
import com.trendradar.trending.domain.AlgorithmTag;
import com.trendradar.trending.domain.TrendingVideo;
import com.trendradar.trending.repository.AlgorithmTagRepository;
import com.trendradar.trending.repository.TrendingVideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("알고리즘 태그 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class AlgorithmTagServiceTest {

    @Mock
    private TrendingVideoRepository trendingVideoRepository;

    @Mock
    private AlgorithmTagRepository algorithmTagRepository;

    @InjectMocks
    private AlgorithmTagService algorithmTagService;

    private final OffsetDateTime now = OffsetDateTime.of(2026, 4, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    // --- SURGE ---

    @Test
    @DisplayName("24시간 조회수 증가 50만 이상이면 SURGE 태그")
    void calculateTags_whenViewIncrease500K_tagsSurge() {
        // Given
        TrendingVideo current = buildVideo("vid1", "KR", 1, 1_500_000L, 0L, 0L,
                now.minusDays(2), now);

        TrendingVideo previous = buildVideo("vid1", "KR", 1, 800_000L, 0L, 0L,
                now.minusDays(2), now.minusDays(1));

        given(trendingVideoRepository.findByVideoIdInAndCollectedAtBetween(
                anyList(), any(), any())).willReturn(List.of(previous));
        given(trendingVideoRepository.findLongRunVideoIds(anyList(), any()))
                .willReturn(Collections.emptyList());
        given(trendingVideoRepository.findGlobalVideoIds(anyList(), any(), any()))
                .willReturn(Collections.emptyList());
        given(algorithmTagRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        // When
        List<AlgorithmTag> tags = algorithmTagService.calculateTags(List.of(current), now);

        // Then
        assertThat(tags).extracting(AlgorithmTag::getTagType)
                .contains(TagType.SURGE.name());
    }

    // --- NEW_ENTRY ---

    @Test
    @DisplayName("업로드 48시간 이내 TOP50 진입이면 NEW_ENTRY 태그")
    void calculateTags_whenUploadWithin48h_tagsNewEntry() {
        // Given
        TrendingVideo video = buildVideo("vid2", "KR", 1, 100_000L, 0L, 0L,
                now.minusHours(24), now);  // 24시간 전 업로드

        given(trendingVideoRepository.findByVideoIdInAndCollectedAtBetween(
                anyList(), any(), any())).willReturn(Collections.emptyList());
        given(trendingVideoRepository.findLongRunVideoIds(anyList(), any()))
                .willReturn(Collections.emptyList());
        given(trendingVideoRepository.findGlobalVideoIds(anyList(), any(), any()))
                .willReturn(Collections.emptyList());
        given(algorithmTagRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        // When
        List<AlgorithmTag> tags = algorithmTagService.calculateTags(List.of(video), now);

        // Then
        assertThat(tags).extracting(AlgorithmTag::getTagType)
                .contains(TagType.NEW_ENTRY.name());
    }

    // --- HOT_COMMENT ---

    @Test
    @DisplayName("댓글/조회수 비율 상위 10%이면 HOT_COMMENT 태그")
    void calculateTags_whenCommentRatioTop10Pct_tagsHotComment() {
        // Given: 10개 영상 중 1개만 댓글 비율이 높음
        List<TrendingVideo> videos = new java.util.ArrayList<>();
        // 높은 댓글 비율 영상 (commentCount/viewCount = 0.1)
        videos.add(buildVideo("hot1", "KR", 1, 100_000L, 0L, 10_000L,
                now.minusDays(10), now));
        // 낮은 댓글 비율 영상 9개 (0.001)
        for (int i = 2; i <= 10; i++) {
            videos.add(buildVideo("low" + i, "KR", i, 100_000L, 0L, 100L,
                    now.minusDays(10), now));
        }

        given(trendingVideoRepository.findByVideoIdInAndCollectedAtBetween(
                anyList(), any(), any())).willReturn(Collections.emptyList());
        given(trendingVideoRepository.findLongRunVideoIds(anyList(), any()))
                .willReturn(Collections.emptyList());
        given(trendingVideoRepository.findGlobalVideoIds(anyList(), any(), any()))
                .willReturn(Collections.emptyList());
        given(algorithmTagRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        // When
        List<AlgorithmTag> tags = algorithmTagService.calculateTags(videos, now);

        // Then
        List<AlgorithmTag> hotCommentTags = tags.stream()
                .filter(t -> t.getTagType().equals(TagType.HOT_COMMENT.name()))
                .toList();
        assertThat(hotCommentTags).hasSize(1);
        assertThat(hotCommentTags.get(0).getVideoId()).isEqualTo("hot1");
    }

    // --- HIGH_ENGAGE ---

    @Test
    @DisplayName("좋아요/조회수 비율 상위 10%이면 HIGH_ENGAGE 태그")
    void calculateTags_whenLikeRatioTop10Pct_tagsHighEngage() {
        // Given: 10개 영상 중 1개만 좋아요 비율이 높음
        List<TrendingVideo> videos = new java.util.ArrayList<>();
        videos.add(buildVideo("eng1", "KR", 1, 100_000L, 20_000L, 0L,
                now.minusDays(10), now));
        for (int i = 2; i <= 10; i++) {
            videos.add(buildVideo("noeng" + i, "KR", i, 100_000L, 100L, 0L,
                    now.minusDays(10), now));
        }

        given(trendingVideoRepository.findByVideoIdInAndCollectedAtBetween(
                anyList(), any(), any())).willReturn(Collections.emptyList());
        given(trendingVideoRepository.findLongRunVideoIds(anyList(), any()))
                .willReturn(Collections.emptyList());
        given(trendingVideoRepository.findGlobalVideoIds(anyList(), any(), any()))
                .willReturn(Collections.emptyList());
        given(algorithmTagRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        // When
        List<AlgorithmTag> tags = algorithmTagService.calculateTags(videos, now);

        // Then
        List<AlgorithmTag> engageTags = tags.stream()
                .filter(t -> t.getTagType().equals(TagType.HIGH_ENGAGE.name()))
                .toList();
        assertThat(engageTags).hasSize(1);
        assertThat(engageTags.get(0).getVideoId()).isEqualTo("eng1");
    }

    // --- LONG_RUN ---

    @Test
    @DisplayName("7일 이상 연속 TOP50이면 LONG_RUN 태그")
    void calculateTags_whenTop50For7Days_tagsLongRun() {
        // Given
        TrendingVideo video = buildVideo("long1", "KR", 1, 500_000L, 0L, 0L,
                now.minusDays(10), now);

        given(trendingVideoRepository.findByVideoIdInAndCollectedAtBetween(
                anyList(), any(), any())).willReturn(Collections.emptyList());
        given(trendingVideoRepository.findLongRunVideoIds(anyList(), any()))
                .willReturn(List.of("long1"));  // DB가 7일 이상 존재 확인
        given(trendingVideoRepository.findGlobalVideoIds(anyList(), any(), any()))
                .willReturn(Collections.emptyList());
        given(algorithmTagRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        // When
        List<AlgorithmTag> tags = algorithmTagService.calculateTags(List.of(video), now);

        // Then
        assertThat(tags).extracting(AlgorithmTag::getTagType)
                .contains(TagType.LONG_RUN.name());
    }

    // --- GLOBAL ---

    @Test
    @DisplayName("3개국 이상 동시 트렌딩이면 GLOBAL 태그")
    void calculateTags_whenTrendingIn3Countries_tagsGlobal() {
        // Given
        TrendingVideo video = buildVideo("glob1", "KR", 1, 500_000L, 0L, 0L,
                now.minusDays(10), now);

        given(trendingVideoRepository.findByVideoIdInAndCollectedAtBetween(
                anyList(), any(), any())).willReturn(Collections.emptyList());
        given(trendingVideoRepository.findLongRunVideoIds(anyList(), any()))
                .willReturn(Collections.emptyList());
        given(trendingVideoRepository.findGlobalVideoIds(anyList(), any(), any()))
                .willReturn(List.of("glob1"));  // DB가 3개국 이상 확인
        given(algorithmTagRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        // When
        List<AlgorithmTag> tags = algorithmTagService.calculateTags(List.of(video), now);

        // Then
        assertThat(tags).extracting(AlgorithmTag::getTagType)
                .contains(TagType.GLOBAL.name());
    }

    // --- COMEBACK ---

    @Test
    @DisplayName("업로드 30일 이후 재진입이면 COMEBACK 태그")
    void calculateTags_whenReentryAfter30Days_tagsComeback() {
        // Given
        TrendingVideo video = buildVideo("come1", "KR", 1, 500_000L, 0L, 0L,
                now.minusDays(45), now);  // 45일 전 업로드

        given(trendingVideoRepository.findByVideoIdInAndCollectedAtBetween(
                anyList(), any(), any())).willReturn(Collections.emptyList());
        given(trendingVideoRepository.findLongRunVideoIds(anyList(), any()))
                .willReturn(Collections.emptyList());
        given(trendingVideoRepository.findGlobalVideoIds(anyList(), any(), any()))
                .willReturn(Collections.emptyList());
        given(algorithmTagRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        // When
        List<AlgorithmTag> tags = algorithmTagService.calculateTags(List.of(video), now);

        // Then
        assertThat(tags).extracting(AlgorithmTag::getTagType)
                .contains(TagType.COMEBACK.name());
    }

    // --- Helper ---

    private TrendingVideo buildVideo(String videoId, String countryCode, int rank,
                                      Long viewCount, Long likeCount, Long commentCount,
                                      OffsetDateTime publishedAt, OffsetDateTime collectedAt) {
        return TrendingVideo.builder()
                .videoId(videoId)
                .title("Test " + videoId)
                .channelTitle("Channel")
                .countryCode(countryCode)
                .rankPosition(rank)
                .viewCount(viewCount)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .publishedAt(publishedAt)
                .collectedAt(collectedAt)
                .build();
    }
}
