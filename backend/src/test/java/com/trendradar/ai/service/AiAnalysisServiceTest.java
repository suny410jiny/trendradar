package com.trendradar.ai.service;

import com.trendradar.ai.domain.AiAnalysis;
import com.trendradar.ai.domain.AnalysisType;
import com.trendradar.ai.dto.AiAnalysisResponse;
import com.trendradar.ai.repository.AiAnalysisRepository;
import com.trendradar.channel.domain.Channel;
import com.trendradar.channel.repository.ChannelRepository;
import com.trendradar.trending.domain.Country;
import com.trendradar.trending.domain.TrendingVideo;
import com.trendradar.trending.repository.CountryRepository;
import com.trendradar.trending.repository.TrendingVideoRepository;
import com.trendradar.youtube.client.ClaudeApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("AI 분석 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private AiAnalysisRepository aiAnalysisRepository;

    @Mock
    private ClaudeApiClient claudeApiClient;

    @Mock
    private TrendingVideoRepository trendingVideoRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private ChannelRepository channelRepository;

    @InjectMocks
    private AiAnalysisService aiAnalysisService;

    @Test
    @DisplayName("브리핑 생성 - 캐시 히트 시 DB에서 반환하고 Claude API 미호출")
    void generateBriefing_whenCacheHit_returnsFromDb() {
        // Given
        AiAnalysis cached = AiAnalysis.builder()
                .analysisType(AnalysisType.BRIEFING)
                .targetId("KR")
                .countryCode("KR")
                .content("캐시된 브리핑 결과")
                .modelUsed("claude-haiku-4-5-20251001")
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1))
                .build();

        given(aiAnalysisRepository.findTopByAnalysisTypeAndTargetIdAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(AnalysisType.BRIEFING), eq("KR"), any(OffsetDateTime.class)))
                .willReturn(Optional.of(cached));

        // When
        AiAnalysisResponse response = aiAnalysisService.generateBriefing("KR");

        // Then
        assertThat(response.isFromCache()).isTrue();
        assertThat(response.getContent()).isEqualTo("캐시된 브리핑 결과");
        assertThat(response.getAnalysisType()).isEqualTo("BRIEFING");
        assertThat(response.getTargetId()).isEqualTo("KR");
        verify(claudeApiClient, never()).generate(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("브리핑 생성 - 캐시 미스 시 Claude API 호출 후 DB 저장")
    void generateBriefing_whenCacheMiss_callsClaudeAndSaves() {
        // Given
        given(aiAnalysisRepository.findTopByAnalysisTypeAndTargetIdAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(AnalysisType.BRIEFING), eq("KR"), any(OffsetDateTime.class)))
                .willReturn(Optional.empty());

        Country korea = Country.of("KR", "한국", "South Korea", "Asia");
        given(countryRepository.findById("KR")).willReturn(Optional.of(korea));

        List<TrendingVideo> videos = List.of(
                TrendingVideo.builder()
                        .videoId("vid1").title("테스트 영상 1").channelTitle("채널1")
                        .countryCode("KR").rankPosition(1)
                        .viewCount(1_000_000L).likeCount(50_000L).commentCount(10_000L)
                        .collectedAt(OffsetDateTime.now(ZoneOffset.UTC))
                        .build()
        );
        given(trendingVideoRepository.findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(
                eq("KR"), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(videos);

        given(claudeApiClient.generate(anyString(), eq("claude-haiku-4-5-20251001"), eq(500)))
                .willReturn("AI 분석 결과");

        given(aiAnalysisRepository.save(any(AiAnalysis.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        AiAnalysisResponse response = aiAnalysisService.generateBriefing("KR");

        // Then
        assertThat(response.isFromCache()).isFalse();
        assertThat(response.getContent()).isEqualTo("AI 분석 결과");
        assertThat(response.getModelUsed()).isEqualTo("claude-haiku-4-5-20251001");
        verify(claudeApiClient).generate(anyString(), eq("claude-haiku-4-5-20251001"), eq(500));
        verify(aiAnalysisRepository).save(any(AiAnalysis.class));
    }

    @Test
    @DisplayName("영상 분석 - Haiku 모델 사용 확인")
    void analyzeVideo_usesHaikuModel() {
        // Given
        given(aiAnalysisRepository.findTopByAnalysisTypeAndTargetIdAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(AnalysisType.VIDEO), eq("vid123"), any(OffsetDateTime.class)))
                .willReturn(Optional.empty());

        TrendingVideo video = TrendingVideo.builder()
                .videoId("vid123").title("인기 영상").channelTitle("인기 채널")
                .countryCode("KR").rankPosition(1)
                .viewCount(5_000_000L).likeCount(200_000L).commentCount(50_000L)
                .collectedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        given(trendingVideoRepository.findByVideoIdIn(List.of("vid123")))
                .willReturn(List.of(video));

        given(claudeApiClient.generate(anyString(), eq("claude-haiku-4-5-20251001"), eq(300)))
                .willReturn("영상 분석 결과");

        given(aiAnalysisRepository.save(any(AiAnalysis.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        AiAnalysisResponse response = aiAnalysisService.analyzeVideo("vid123");

        // Then
        verify(claudeApiClient).generate(anyString(), eq("claude-haiku-4-5-20251001"), eq(300));
        assertThat(response.getContent()).isEqualTo("영상 분석 결과");
    }

    @Test
    @DisplayName("채널 분석 - Sonnet 모델 사용 확인")
    void analyzeChannel_usesSonnetModel() {
        // Given
        given(aiAnalysisRepository.findTopByAnalysisTypeAndTargetIdAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(AnalysisType.CHANNEL), eq("UC_test"), any(OffsetDateTime.class)))
                .willReturn(Optional.empty());

        Channel channel = Channel.builder()
                .channelId("UC_test").title("테스트 채널")
                .subscriberCount(100_000L).videoCount(200L).totalViewCount(50_000_000L)
                .build();
        given(channelRepository.findByChannelIdIn(List.of("UC_test")))
                .willReturn(List.of(channel));

        given(claudeApiClient.generate(anyString(), eq("claude-sonnet-4-20250514"), eq(500)))
                .willReturn("채널 분석 결과");

        given(aiAnalysisRepository.save(any(AiAnalysis.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        AiAnalysisResponse response = aiAnalysisService.analyzeChannel("UC_test");

        // Then
        verify(claudeApiClient).generate(anyString(), eq("claude-sonnet-4-20250514"), eq(500));
        assertThat(response.getContent()).isEqualTo("채널 분석 결과");
    }
}
