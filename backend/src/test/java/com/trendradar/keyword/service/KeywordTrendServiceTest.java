package com.trendradar.keyword.service;

import com.trendradar.keyword.domain.KeywordTrend;
import com.trendradar.keyword.domain.PeriodType;
import com.trendradar.keyword.repository.KeywordTrendRepository;
import com.trendradar.trending.domain.TrendingVideo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("키워드 트렌드 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class KeywordTrendServiceTest {

    @Mock
    private KeywordTrendRepository keywordTrendRepository;

    @InjectMocks
    private KeywordTrendService keywordTrendService;

    private static final OffsetDateTime COLLECTED_AT =
            OffsetDateTime.of(2026, 4, 1, 14, 30, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime PERIOD_START =
            COLLECTED_AT.truncatedTo(ChronoUnit.HOURS);

    @Test
    @DisplayName("2개 영상의 중복 태그 추출 시 빈도수 올바르게 집계")
    void aggregateHourlyKeywords_extractsTagsAndSavesFrequency() {
        // Given - 2 videos with overlapping tags
        TrendingVideo video1 = TrendingVideo.builder()
                .videoId("vid_001")
                .title("먹방 영상")
                .channelTitle("먹방채널")
                .countryCode("KR")
                .rankPosition(1)
                .viewCount(1_000_000L)
                .likeCount(50_000L)
                .commentCount(2_000L)
                .collectedAt(COLLECTED_AT)
                .youtubeTags("먹방,ASMR,치킨")
                .build();

        TrendingVideo video2 = TrendingVideo.builder()
                .videoId("vid_002")
                .title("ASMR 영상")
                .channelTitle("ASMR채널")
                .countryCode("KR")
                .rankPosition(2)
                .viewCount(500_000L)
                .likeCount(30_000L)
                .commentCount(1_000L)
                .collectedAt(COLLECTED_AT)
                .youtubeTags("ASMR,수면")
                .build();

        List<TrendingVideo> videos = List.of(video1, video2);

        // No existing keyword trends
        given(keywordTrendRepository.findByKeywordAndCountryCodeAndPeriodTypeAndPeriodStart(
                anyString(), eq("KR"), eq(PeriodType.HOUR), eq(PERIOD_START)))
                .willReturn(Optional.empty());
        given(keywordTrendRepository.save(any(KeywordTrend.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordTrendService.aggregateHourlyKeywords(videos, "KR", COLLECTED_AT);

        // Then - 4 unique keywords: 먹방, asmr, 치킨, 수면
        ArgumentCaptor<KeywordTrend> captor = ArgumentCaptor.forClass(KeywordTrend.class);
        verify(keywordTrendRepository, times(4)).save(captor.capture());

        List<KeywordTrend> savedTrends = captor.getAllValues();

        // "asmr" appears in both videos → videoCount=2
        KeywordTrend asmrTrend = savedTrends.stream()
                .filter(t -> t.getKeyword().equals("asmr"))
                .findFirst().orElseThrow();
        assertThat(asmrTrend.getVideoCount()).isEqualTo(2);
        assertThat(asmrTrend.getTotalViews()).isEqualTo(1_500_000L); // 1M + 500K
        assertThat(asmrTrend.getCountryCode()).isEqualTo("KR");
        assertThat(asmrTrend.getPeriodType()).isEqualTo(PeriodType.HOUR);
        assertThat(asmrTrend.getPeriodStart()).isEqualTo(PERIOD_START);
        // avgEngagement = avg of (50000/1000000, 30000/500000) = avg(0.05, 0.06) = 0.055
        assertThat(asmrTrend.getAvgEngagement()).isCloseTo(0.055, org.assertj.core.api.Assertions.within(0.001));

        // "먹방" appears in 1 video → videoCount=1
        KeywordTrend mukbangTrend = savedTrends.stream()
                .filter(t -> t.getKeyword().equals("먹방"))
                .findFirst().orElseThrow();
        assertThat(mukbangTrend.getVideoCount()).isEqualTo(1);
        assertThat(mukbangTrend.getTotalViews()).isEqualTo(1_000_000L);

        // "치킨" appears in 1 video → videoCount=1
        KeywordTrend chickenTrend = savedTrends.stream()
                .filter(t -> t.getKeyword().equals("치킨"))
                .findFirst().orElseThrow();
        assertThat(chickenTrend.getVideoCount()).isEqualTo(1);

        // "수면" appears in 1 video → videoCount=1
        KeywordTrend sleepTrend = savedTrends.stream()
                .filter(t -> t.getKeyword().equals("수면"))
                .findFirst().orElseThrow();
        assertThat(sleepTrend.getVideoCount()).isEqualTo(1);
        assertThat(sleepTrend.getTotalViews()).isEqualTo(500_000L);
    }

    @Test
    @DisplayName("태그가 없는 영상은 키워드 저장하지 않음")
    void aggregateHourlyKeywords_whenNoTags_doesNothing() {
        // Given - video with null tags
        TrendingVideo video = TrendingVideo.builder()
                .videoId("vid_003")
                .title("태그 없는 영상")
                .channelTitle("채널")
                .countryCode("KR")
                .rankPosition(1)
                .viewCount(100_000L)
                .likeCount(5_000L)
                .commentCount(500L)
                .collectedAt(COLLECTED_AT)
                .youtubeTags(null)
                .build();

        List<TrendingVideo> videos = List.of(video);

        // When
        keywordTrendService.aggregateHourlyKeywords(videos, "KR", COLLECTED_AT);

        // Then - no save should happen
        verify(keywordTrendRepository, never()).save(any(KeywordTrend.class));
        verify(keywordTrendRepository, never())
                .findByKeywordAndCountryCodeAndPeriodTypeAndPeriodStart(
                        anyString(), anyString(), any(), any());
    }
}
