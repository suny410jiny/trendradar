package com.trendradar.channel.controller;

import com.trendradar.ai.dto.AiAnalysisResponse;
import com.trendradar.ai.service.AiAnalysisService;
import com.trendradar.channel.domain.Channel;
import com.trendradar.channel.domain.ChannelSnapshot;
import com.trendradar.channel.dto.ChannelDetailResponse;
import com.trendradar.channel.dto.ChannelRankingResponse;
import com.trendradar.channel.repository.ChannelRepository;
import com.trendradar.channel.repository.ChannelSnapshotRepository;
import com.trendradar.channel.service.ChannelRankingService;
import com.trendradar.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v2/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelRankingService channelRankingService;
    private final ChannelRepository channelRepository;
    private final ChannelSnapshotRepository channelSnapshotRepository;
    private final AiAnalysisService aiAnalysisService;

    @GetMapping("/ranking")
    public ApiResponse<List<ChannelRankingResponse>> getRanking(
            @RequestParam String country,
            @RequestParam(defaultValue = "100") int limit) {
        List<ChannelRankingResponse> ranking = channelRankingService.getRanking(country, limit);
        return ApiResponse.of(ranking);
    }

    @GetMapping("/{channelId}")
    public ApiResponse<ChannelDetailResponse> getDetail(@PathVariable String channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NoSuchElementException("Channel not found: " + channelId));

        // Try to get AI analysis (may return null/cached)
        AiAnalysisResponse aiAnalysis = null;
        try {
            aiAnalysis = aiAnalysisService.analyzeChannel(channelId);
        } catch (Exception e) {
            // AI analysis is optional, don't fail the request
        }

        // Get ranking info for this channel
        List<ChannelRankingResponse> allRanking = channelRankingService.getRanking("ALL", 200);
        ChannelRankingResponse rankInfo = allRanking.stream()
                .filter(r -> r.getChannelId().equals(channelId))
                .findFirst()
                .orElse(null);

        ChannelDetailResponse response = ChannelDetailResponse.builder()
                .channelId(channel.getChannelId())
                .title(channel.getTitle())
                .thumbnailUrl(channel.getThumbnailUrl())
                .subscriberCount(channel.getSubscriberCount())
                .videoCount(channel.getVideoCount())
                .totalViewCount(channel.getTotalViewCount())
                .surgeScore(rankInfo != null ? rankInfo.getSurgeScore() : 0)
                .grade(rankInfo != null ? rankInfo.getGrade() : "D")
                .gradeLabel(rankInfo != null ? rankInfo.getGradeLabel() : "트렌딩에 등장한 채널")
                .darkhorse(rankInfo != null && rankInfo.isDarkhorse())
                .firstSeenAt(channel.getFirstSeenAt())
                .aiAnalysis(aiAnalysis)
                .build();
        return ApiResponse.of(response);
    }

    @GetMapping("/{channelId}/snapshots")
    public ApiResponse<List<ChannelSnapshot>> getSnapshots(@PathVariable String channelId) {
        List<ChannelSnapshot> snapshots = channelSnapshotRepository
                .findByChannelIdOrderBySnapshotAtDesc(channelId);
        return ApiResponse.of(snapshots);
    }
}
