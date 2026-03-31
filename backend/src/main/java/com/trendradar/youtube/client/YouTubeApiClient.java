package com.trendradar.youtube.client;

import com.trendradar.youtube.dto.YouTubeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class YouTubeApiClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public YouTubeApiClient(
            @Value("${youtube.api.key}") String apiKey,
            @Value("${youtube.api.base-url:https://www.googleapis.com}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public YouTubeResponse fetchTrendingVideos(String regionCode, int maxResults) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/youtube/v3/videos")
                .queryParam("part", "snippet,contentDetails,statistics")
                .queryParam("chart", "mostPopular")
                .queryParam("regionCode", regionCode)
                .queryParam("maxResults", maxResults)
                .queryParam("key", apiKey)
                .toUriString();

        log.info("Fetching trending videos for region={}, maxResults={}", regionCode, maxResults);

        return restTemplate.getForObject(url, YouTubeResponse.class);
    }
}
