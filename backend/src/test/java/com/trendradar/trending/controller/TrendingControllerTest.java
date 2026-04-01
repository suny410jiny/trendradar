package com.trendradar.trending.controller;

import com.trendradar.trending.dto.CategoryResponse;
import com.trendradar.trending.dto.CountryResponse;
import com.trendradar.trending.dto.TrendingVideoResponse;
import com.trendradar.trending.dto.ViewSnapshotResponse;
import com.trendradar.trending.service.TrendingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrendingController.class)
@DisplayName("트렌딩 API 컨트롤러 테스트")
class TrendingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrendingService trendingService;

    // --- GET /api/v1/trending ---

    @Test
    @DisplayName("유효한 국가 코드로 트렌딩 조회 시 200")
    void getTrending_whenValidCountry_returns200() throws Exception {
        // Given
        TrendingVideoResponse video = TrendingVideoResponse.builder()
                .videoId("v1").title("Test").channelTitle("Ch").countryCode("KR")
                .rankPosition(1).viewCount(100L).tags(List.of("SURGE"))
                .build();

        given(trendingService.getTrending(eq("KR"), isNull(), isNull(), eq(10)))
                .willReturn(List.of(video));

        // When & Then
        mockMvc.perform(get("/api/v1/trending").param("country", "KR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].videoId").value("v1"))
                .andExpect(jsonPath("$.data[0].tags[0]").value("SURGE"));
    }

    @Test
    @DisplayName("국가 코드 누락 시 400")
    void getTrending_whenMissingCountry_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/trending"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/v1/trending/{videoId}/snapshots ---

    @Test
    @DisplayName("유효한 videoId로 스냅샷 조회 시 200")
    void getSnapshots_whenValidVideoId_returns200() throws Exception {
        // Given
        ViewSnapshotResponse snapshot = ViewSnapshotResponse.builder()
                .videoId("v1").viewCount(100_000L)
                .snapshotAt(OffsetDateTime.now())
                .build();

        given(trendingService.getSnapshots("v1")).willReturn(List.of(snapshot));

        // When & Then
        mockMvc.perform(get("/api/v1/trending/v1/snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].videoId").value("v1"));
    }

    @Test
    @DisplayName("존재하지 않는 videoId로 스냅샷 조회 시 404")
    void getSnapshots_whenVideoNotFound_returns404() throws Exception {
        // Given
        given(trendingService.getSnapshots("unknown"))
                .willThrow(new NoSuchElementException("Video snapshots not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/trending/unknown/snapshots"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // --- GET /api/v1/countries ---

    @Test
    @DisplayName("국가 목록 조회 시 200")
    void getCountries_returns200WithList() throws Exception {
        // Given
        CountryResponse kr = CountryResponse.builder()
                .code("KR").nameKo("한국").nameEn("South Korea").region("Asia")
                .build();

        given(trendingService.getCountries()).willReturn(List.of(kr));

        // When & Then
        mockMvc.perform(get("/api/v1/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("KR"));
    }

    // --- GET /api/v1/categories ---

    @Test
    @DisplayName("카테고리 목록 조회 시 200")
    void getCategories_returns200WithList() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].name").value("음악"));
    }
}
