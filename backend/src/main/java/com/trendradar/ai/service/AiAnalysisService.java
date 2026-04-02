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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AiAnalysisService {

    private static final String MODEL_HAIKU = "claude-haiku-4-5-20251001";
    private static final String MODEL_SONNET = "claude-sonnet-4-20250514";

    private final AiAnalysisRepository aiAnalysisRepository;
    private final ClaudeApiClient claudeApiClient;
    private final TrendingVideoRepository trendingVideoRepository;
    private final CountryRepository countryRepository;
    private final ChannelRepository channelRepository;

    /**
     * 트렌드 브리핑 생성 (BRIEFING)
     * - Model: Haiku (속도/비용 최적화)
     * - TTL: 1시간
     * - maxTokens: 500
     */
    @Transactional
    public AiAnalysisResponse generateBriefing(String countryCode) {
        // 1. 캐시 확인
        Optional<AiAnalysis> cached = findCached(AnalysisType.BRIEFING, countryCode);
        if (cached.isPresent()) {
            log.info("Briefing cache HIT for country={}", countryCode);
            return toResponse(cached.get(), true);
        }

        // 2. 프롬프트 빌드
        String countryName = countryRepository.findById(countryCode)
                .map(Country::getNameKo)
                .orElse(countryCode);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<TrendingVideo> videos = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(
                        countryCode, now.minusHours(6), now);

        List<TrendingVideo> top10 = videos.stream().limit(10).toList();

        String videoList = top10.stream()
                .map(v -> String.format("%d. \"%s\" (채널: %s, 조회수: %s)",
                        v.getRankPosition(), v.getTitle(), v.getChannelTitle(),
                        formatViewCount(v.getViewCount())))
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                당신은 YouTube 트렌드 분석가입니다. 긍정적이고 재미있는 톤으로 분석해주세요.
                다음은 %s YouTube 트렌딩 TOP 10 영상입니다:
                %s

                3줄로 오늘의 트렌드를 요약해주세요:
                1. 가장 주목할 만한 트렌드
                2. 눈에 띄는 채널이나 영상
                3. 내일 주목할 만한 흐름""", countryName, videoList);

        // 3. Claude API 호출
        String content = claudeApiClient.generate(prompt, MODEL_HAIKU, 500);

        // 4. DB 저장
        AiAnalysis analysis = saveAnalysis(AnalysisType.BRIEFING, countryCode, countryCode,
                content, MODEL_HAIKU, 1);

        log.info("Briefing generated for country={}", countryCode);
        return toResponse(analysis, false);
    }

    /**
     * 영상 분석 (VIDEO)
     * - Model: Haiku
     * - TTL: 24시간
     * - maxTokens: 300
     */
    @Transactional
    public AiAnalysisResponse analyzeVideo(String videoId) {
        // 1. 캐시 확인
        Optional<AiAnalysis> cached = findCached(AnalysisType.VIDEO, videoId);
        if (cached.isPresent()) {
            log.info("Video analysis cache HIT for videoId={}", videoId);
            return toResponse(cached.get(), true);
        }

        // 2. 영상 데이터 조회
        List<TrendingVideo> videos = trendingVideoRepository.findByVideoIdIn(List.of(videoId));
        TrendingVideo video = videos.stream().findFirst().orElse(null);

        String prompt;
        if (video != null) {
            prompt = String.format("""
                    다음 YouTube 트렌딩 영상을 분석해주세요:
                    - 제목: %s
                    - 채널: %s
                    - 조회수: %s
                    - 좋아요: %s
                    - 댓글: %s

                    이 영상이 트렌딩에 오른 이유와 콘텐츠 전략을 2-3줄로 분석해주세요.""",
                    video.getTitle(), video.getChannelTitle(),
                    formatViewCount(video.getViewCount()),
                    formatViewCount(video.getLikeCount()),
                    formatViewCount(video.getCommentCount()));
        } else {
            prompt = String.format("YouTube 영상 ID '%s'에 대한 간단한 트렌드 분석을 제공해주세요.", videoId);
        }

        // 3. Claude API 호출
        String content = claudeApiClient.generate(prompt, MODEL_HAIKU, 300);

        // 4. DB 저장
        String countryCode = video != null ? video.getCountryCode() : null;
        AiAnalysis analysis = saveAnalysis(AnalysisType.VIDEO, videoId, countryCode,
                content, MODEL_HAIKU, 24);

        log.info("Video analysis generated for videoId={}", videoId);
        return toResponse(analysis, false);
    }

    /**
     * 채널 분석 (CHANNEL)
     * - Model: Sonnet (더 깊은 분석)
     * - TTL: 6시간
     * - maxTokens: 500
     */
    @Transactional
    public AiAnalysisResponse analyzeChannel(String channelId) {
        // 1. 캐시 확인
        Optional<AiAnalysis> cached = findCached(AnalysisType.CHANNEL, channelId);
        if (cached.isPresent()) {
            log.info("Channel analysis cache HIT for channelId={}", channelId);
            return toResponse(cached.get(), true);
        }

        // 2. 채널 데이터 조회
        List<Channel> channels = channelRepository.findByChannelIdIn(List.of(channelId));
        Channel channel = channels.stream().findFirst().orElse(null);

        String prompt;
        if (channel != null) {
            prompt = String.format("""
                    다음 YouTube 채널의 트렌드 영향력을 분석해주세요:
                    - 채널명: %s
                    - 구독자: %s
                    - 총 조회수: %s
                    - 영상 수: %d

                    이 채널의 성장 전략과 트렌딩 패턴을 3줄로 분석해주세요.""",
                    channel.getTitle(),
                    formatViewCount(channel.getSubscriberCount()),
                    formatViewCount(channel.getTotalViewCount()),
                    channel.getVideoCount() != null ? channel.getVideoCount() : 0);
        } else {
            prompt = String.format("YouTube 채널 ID '%s'에 대한 트렌드 분석을 제공해주세요.", channelId);
        }

        // 3. Claude API 호출
        String content = claudeApiClient.generate(prompt, MODEL_SONNET, 500);

        // 4. DB 저장
        AiAnalysis analysis = saveAnalysis(AnalysisType.CHANNEL, channelId, null,
                content, MODEL_SONNET, 6);

        log.info("Channel analysis generated for channelId={}", channelId);
        return toResponse(analysis, false);
    }

    /**
     * 트렌드 예측 (PREDICTION)
     * - Model: Sonnet (고품질 분석)
     * - TTL: 6시간
     * - maxTokens: 500
     */
    @Transactional
    public AiAnalysisResponse generatePrediction(String countryCode) {
        // 1. 캐시 확인
        Optional<AiAnalysis> cached = findCached(AnalysisType.PREDICTION, countryCode);
        if (cached.isPresent()) {
            log.info("Prediction cache HIT for country={}", countryCode);
            return toResponse(cached.get(), true);
        }

        // 2. 프롬프트 빌드
        String countryName = countryRepository.findById(countryCode)
                .map(Country::getNameKo)
                .orElse(countryCode);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<TrendingVideo> videos = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(
                        countryCode, now.minusHours(24), now);

        List<TrendingVideo> top20 = videos.stream().limit(20).toList();

        String videoSummary = top20.stream()
                .map(v -> String.format("- \"%s\" (조회수: %s)",
                        v.getTitle(), formatViewCount(v.getViewCount())))
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                당신은 YouTube 트렌드 예측 전문가입니다.
                다음은 %s의 최근 24시간 트렌딩 영상입니다:
                %s

                향후 트렌드를 예측해주세요:
                1. 다음 24시간 내 떠오를 콘텐츠 유형
                2. 주목할 키워드나 주제
                3. 크리에이터를 위한 제안""", countryName, videoSummary);

        // 3. Claude API 호출
        String content = claudeApiClient.generate(prompt, MODEL_SONNET, 500);

        // 4. DB 저장
        AiAnalysis analysis = saveAnalysis(AnalysisType.PREDICTION, countryCode, countryCode,
                content, MODEL_SONNET, 6);

        log.info("Prediction generated for country={}", countryCode);
        return toResponse(analysis, false);
    }

    // ── Private Helper Methods ──────────────────────────────

    private Optional<AiAnalysis> findCached(AnalysisType type, String targetId) {
        return aiAnalysisRepository
                .findTopByAnalysisTypeAndTargetIdAndExpiresAtAfterOrderByCreatedAtDesc(
                        type, targetId, OffsetDateTime.now(ZoneOffset.UTC));
    }

    private AiAnalysis saveAnalysis(AnalysisType type, String targetId, String countryCode,
                                     String content, String model, int ttlHours) {
        AiAnalysis analysis = AiAnalysis.builder()
                .analysisType(type)
                .targetId(targetId)
                .countryCode(countryCode)
                .content(content)
                .modelUsed(model)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(ttlHours))
                .build();
        return aiAnalysisRepository.save(analysis);
    }

    private AiAnalysisResponse toResponse(AiAnalysis analysis, boolean fromCache) {
        return AiAnalysisResponse.builder()
                .analysisType(analysis.getAnalysisType().name())
                .targetId(analysis.getTargetId())
                .content(analysis.getContent())
                .modelUsed(analysis.getModelUsed())
                .createdAt(analysis.getCreatedAt())
                .fromCache(fromCache)
                .build();
    }

    private String formatViewCount(Long count) {
        if (count == null) return "0";
        if (count >= 100_000_000) return String.format("%.1f억", count / 100_000_000.0);
        if (count >= 10_000) return String.format("%.1f만", count / 10_000.0);
        return String.format("%,d", count);
    }
}
