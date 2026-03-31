package com.trendradar.scheduler;

import com.trendradar.tag.service.AlgorithmTagService;
import com.trendradar.trending.domain.TrendingVideo;
import com.trendradar.trending.service.TrendingCollectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("트렌딩 스케줄러 테스트")
@ExtendWith(MockitoExtension.class)
class TrendingSchedulerTest {

    @Mock
    private TrendingCollectService trendingCollectService;

    @Mock
    private AlgorithmTagService algorithmTagService;

    @InjectMocks
    private TrendingScheduler trendingScheduler;

    @Test
    @DisplayName("스케줄러 실행 시 5개국 수집 호출")
    void collectAllCountries_callsCollectFor5Countries() {
        // Given
        given(trendingCollectService.collectAsync(anyString()))
                .willReturn(CompletableFuture.completedFuture(List.of()));

        // When
        trendingScheduler.collectAllCountries();

        // Then
        verify(trendingCollectService).collectAsync("KR");
        verify(trendingCollectService).collectAsync("US");
        verify(trendingCollectService).collectAsync("JP");
        verify(trendingCollectService).collectAsync("GB");
        verify(trendingCollectService).collectAsync("DE");
        verify(trendingCollectService, times(5)).collectAsync(anyString());
    }

    @Test
    @DisplayName("수집 완료 후 알고리즘 태그 계산 트리거")
    void collectAllCountries_triggersTagCalculation() {
        // Given
        TrendingVideo video = TrendingVideo.builder()
                .videoId("v1").title("test").countryCode("KR")
                .rankPosition(1).viewCount(100L).collectedAt(java.time.OffsetDateTime.now())
                .build();

        given(trendingCollectService.collectAsync(anyString()))
                .willReturn(CompletableFuture.completedFuture(List.of(video)));

        // When
        trendingScheduler.collectAllCountries();

        // Then
        verify(algorithmTagService).calculateTags(anyList(), any());
    }

    @Test
    @DisplayName("1개 국가 수집 실패해도 나머지 계속 진행")
    void collectAllCountries_whenOneCountryFails_continuesOthers() {
        // Given
        CompletableFuture<List<TrendingVideo>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("API Error"));

        given(trendingCollectService.collectAsync("KR")).willReturn(failedFuture);
        given(trendingCollectService.collectAsync("US"))
                .willReturn(CompletableFuture.completedFuture(List.of()));
        given(trendingCollectService.collectAsync("JP"))
                .willReturn(CompletableFuture.completedFuture(List.of()));
        given(trendingCollectService.collectAsync("GB"))
                .willReturn(CompletableFuture.completedFuture(List.of()));
        given(trendingCollectService.collectAsync("DE"))
                .willReturn(CompletableFuture.completedFuture(List.of()));

        // When - 예외 발생하지 않아야 함
        trendingScheduler.collectAllCountries();

        // Then - 5개국 모두 호출됨
        verify(trendingCollectService, times(5)).collectAsync(anyString());
    }
}
