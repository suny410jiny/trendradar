package com.trendradar.trending.service;

import com.trendradar.trending.domain.Country;
import com.trendradar.trending.dto.BriefingResponse;
import com.trendradar.trending.dto.TrendingVideoResponse;
import com.trendradar.trending.repository.CountryRepository;
import com.trendradar.youtube.client.ClaudeApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@DisplayName("브리핑 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class BriefingServiceTest {

    @Mock
    private TrendingService trendingService;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private ClaudeApiClient claudeApiClient;

    @InjectMocks
    private BriefingService briefingService;

    @Test
    @DisplayName("유효한 국가 코드로 브리핑 생성 시 요약 포함")
    void generateBriefing_whenValidCountry_returnsSummary() {
        // Given
        Country korea = Country.of("KR", "한국", "South Korea", "Asia");
        given(countryRepository.findById("KR")).willReturn(Optional.of(korea));

        TrendingVideoResponse video = TrendingVideoResponse.builder()
                .videoId("v1").title("BTS 신곡").channelTitle("HYBE")
                .countryCode("KR").rankPosition(1).viewCount(5_000_000L)
                .tags(List.of())
                .build();
        given(trendingService.getTrending("KR", null, 10)).willReturn(List.of(video));
        given(claudeApiClient.generateBriefing(anyString()))
                .willReturn("한국 YouTube에서 K-POP이 강세입니다.");

        // When
        BriefingResponse result = briefingService.generateBriefing("KR");

        // Then
        assertThat(result.getCountry()).isEqualTo("KR");
        assertThat(result.getCountryName()).isEqualTo("한국");
        assertThat(result.getSummary()).contains("K-POP");
        assertThat(result.getTopVideos()).hasSize(1);
        assertThat(result.getGeneratedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 국가 코드 시 NoSuchElementException")
    void generateBriefing_whenInvalidCountry_throwsException() {
        // Given
        given(countryRepository.findById("XX")).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> briefingService.generateBriefing("XX"))
                .isInstanceOf(NoSuchElementException.class);
    }
}
