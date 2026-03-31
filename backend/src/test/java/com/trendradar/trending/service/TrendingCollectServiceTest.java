package com.trendradar.trending.service;

import com.trendradar.trending.domain.TrendingVideo;
import com.trendradar.trending.repository.TrendingVideoRepository;
import com.trendradar.youtube.client.YouTubeApiClient;
import com.trendradar.youtube.dto.YouTubeResponse;
import com.trendradar.youtube.dto.YouTubeVideoItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("트렌딩 수집 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class TrendingCollectServiceTest {

    @Mock
    private YouTubeApiClient youTubeApiClient;

    @Mock
    private TrendingVideoRepository trendingVideoRepository;

    @InjectMocks
    private TrendingCollectService trendingCollectService;

    @Test
    @DisplayName("한국 트렌딩 수집 시 DB에 영상 저장")
    void collectTrending_whenKorea_savesVideos() {
        // Given
        YouTubeResponse mockResponse = createMockResponse();
        given(youTubeApiClient.fetchTrendingVideos("KR", 50)).willReturn(mockResponse);
        given(trendingVideoRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        // When
        List<TrendingVideo> result = trendingCollectService.collect("KR");

        // Then
        assertThat(result).hasSize(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TrendingVideo>> captor = ArgumentCaptor.forClass(List.class);
        verify(trendingVideoRepository).saveAll(captor.capture());

        List<TrendingVideo> saved = captor.getValue();
        assertThat(saved.get(0).getVideoId()).isEqualTo("video1");
        assertThat(saved.get(0).getTitle()).isEqualTo("테스트 영상 1");
        assertThat(saved.get(0).getCountryCode()).isEqualTo("KR");
        assertThat(saved.get(0).getRankPosition()).isEqualTo(1);
        assertThat(saved.get(0).getViewCount()).isEqualTo(1000000L);
    }

    @Test
    @DisplayName("YouTube API 응답이 null이면 빈 리스트 반환")
    void collectTrending_whenNullResponse_returnsEmpty() {
        // Given
        given(youTubeApiClient.fetchTrendingVideos("KR", 50)).willReturn(null);

        // When
        List<TrendingVideo> result = trendingCollectService.collect("KR");

        // Then
        assertThat(result).isEmpty();
    }

    private YouTubeResponse createMockResponse() {
        // Jackson ObjectMapper로 직접 생성하기 어려우므로 리플렉션 사용
        try {
            YouTubeResponse response = new YouTubeResponse();
            var itemsField = YouTubeResponse.class.getDeclaredField("items");
            itemsField.setAccessible(true);

            YouTubeVideoItem item1 = createMockItem("video1", "테스트 영상 1", "채널1", "10",
                    "1000000", "50000", "10000", "PT5M", "2026-03-31T10:00:00Z");
            YouTubeVideoItem item2 = createMockItem("video2", "테스트 영상 2", "채널2", "24",
                    "500000", "20000", "5000", "PT3M", "2026-03-30T08:00:00Z");

            itemsField.set(response, List.of(item1, item2));
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private YouTubeVideoItem createMockItem(String id, String title, String channelTitle,
                                             String categoryId, String viewCount, String likeCount,
                                             String commentCount, String duration, String publishedAt) {
        try {
            YouTubeVideoItem item = new YouTubeVideoItem();
            setField(item, "id", id);

            YouTubeVideoItem.Snippet snippet = new YouTubeVideoItem.Snippet();
            setField(snippet, "title", title);
            setField(snippet, "channelTitle", channelTitle);
            setField(snippet, "categoryId", categoryId);
            setField(snippet, "publishedAt", publishedAt);
            setField(item, "snippet", snippet);

            YouTubeVideoItem.Statistics stats = new YouTubeVideoItem.Statistics();
            setField(stats, "viewCount", viewCount);
            setField(stats, "likeCount", likeCount);
            setField(stats, "commentCount", commentCount);
            setField(item, "statistics", stats);

            YouTubeVideoItem.ContentDetails content = new YouTubeVideoItem.ContentDetails();
            setField(content, "duration", duration);
            setField(item, "contentDetails", content);

            return item;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
