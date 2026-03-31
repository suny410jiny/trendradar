package com.trendradar.youtube.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.trendradar.youtube.dto.YouTubeResponse;
import com.trendradar.youtube.dto.YouTubeVideoItem;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("YouTube API Client 테스트")
class YouTubeApiClientTest {

    private static WireMockServer wireMockServer;
    private YouTubeApiClient youTubeApiClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort()
                .usingFilesUnderDirectory("src/test/resources"));
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        youTubeApiClient = new YouTubeApiClient(
                "test-api-key",
                "http://localhost:" + wireMockServer.port());
    }

    @Test
    @DisplayName("한국 트렌딩 요청 시 영상 목록 파싱 성공")
    void fetchTrendingVideos_whenKorea_parsesCorrectly() {
        // Given
        wireMockServer.stubFor(get(urlPathEqualTo("/youtube/v3/videos"))
                .withQueryParam("regionCode", equalTo("KR"))
                .withQueryParam("chart", equalTo("mostPopular"))
                .withQueryParam("key", equalTo("test-api-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("youtube_trending_kr.json")));

        // When
        YouTubeResponse response = youTubeApiClient.fetchTrendingVideos("KR", 50);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);

        YouTubeVideoItem first = response.getItems().get(0);
        assertThat(first.getId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(first.getSnippet().getTitle()).isEqualTo("테스트 영상 제목 1");
        assertThat(first.getSnippet().getChannelTitle()).isEqualTo("테스트 채널");
        assertThat(first.getSnippet().getCategoryId()).isEqualTo("10");
        assertThat(first.getStatistics().getViewCount()).isEqualTo("1500000");
        assertThat(first.getContentDetails().getDuration()).isEqualTo("PT3M33S");
    }

    @Test
    @DisplayName("API 응답에 thumbnails 포함 시 URL 파싱")
    void fetchTrendingVideos_whenHasThumbnail_parsesThumbnailUrl() {
        // Given
        wireMockServer.stubFor(get(urlPathEqualTo("/youtube/v3/videos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("youtube_trending_kr.json")));

        // When
        YouTubeResponse response = youTubeApiClient.fetchTrendingVideos("KR", 50);

        // Then
        YouTubeVideoItem first = response.getItems().get(0);
        assertThat(first.getSnippet().getThumbnails().getHigh().getUrl())
                .contains("hqdefault.jpg");
    }
}
