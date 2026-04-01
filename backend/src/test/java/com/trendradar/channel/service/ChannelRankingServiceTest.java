package com.trendradar.channel.service;

import com.trendradar.channel.domain.Channel;
import com.trendradar.channel.dto.ChannelRankingResponse;
import com.trendradar.channel.repository.ChannelRepository;
import com.trendradar.trending.domain.TrendingVideo;
import com.trendradar.trending.repository.TrendingVideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@DisplayName("채널 랭킹 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class ChannelRankingServiceTest {

    @Mock
    private TrendingVideoRepository trendingVideoRepository;

    @Mock
    private ChannelRepository channelRepository;

    @InjectMocks
    private ChannelRankingService channelRankingService;

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 4, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    @DisplayName("여러 채널이 있을 때 surgeScore 내림차순 정렬")
    void getRanking_withMultipleChannels_returnsSortedByScore() {
        // Given
        // 채널 3개: ch1(높은 점수), ch2(중간 점수), ch3(낮은 점수)
        List<String> channelIds = List.of("UC_ch1", "UC_ch2", "UC_ch3");

        given(trendingVideoRepository.findDistinctChannelIdsByCountryCode(
                eq("KR"), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(channelIds);

        // ch1: rank 1, 고조회/고참여 → 최고 점수
        TrendingVideo ch1Video1 = TrendingVideo.builder()
                .videoId("vid1").channelId("UC_ch1").channelTitle("TopChannel")
                .countryCode("KR").rankPosition(1)
                .viewCount(5_000_000L).likeCount(200_000L).commentCount(50_000L)
                .collectedAt(NOW).build();
        TrendingVideo ch1Video2 = TrendingVideo.builder()
                .videoId("vid2").channelId("UC_ch1").channelTitle("TopChannel")
                .countryCode("KR").rankPosition(3)
                .viewCount(3_000_000L).likeCount(150_000L).commentCount(30_000L)
                .collectedAt(NOW).build();

        // ch2: rank 10, 중간 조회/참여
        TrendingVideo ch2Video = TrendingVideo.builder()
                .videoId("vid3").channelId("UC_ch2").channelTitle("MidChannel")
                .countryCode("KR").rankPosition(10)
                .viewCount(500_000L).likeCount(10_000L).commentCount(1_000L)
                .collectedAt(NOW).build();

        // ch3: rank 45, 낮은 조회/참여
        TrendingVideo ch3Video = TrendingVideo.builder()
                .videoId("vid4").channelId("UC_ch3").channelTitle("LowChannel")
                .countryCode("KR").rankPosition(45)
                .viewCount(50_000L).likeCount(500L).commentCount(50L)
                .collectedAt(NOW).build();

        List<TrendingVideo> allVideos = List.of(ch1Video1, ch1Video2, ch2Video, ch3Video);

        given(trendingVideoRepository.findByChannelIdInAndCollectedAtBetween(
                eq(channelIds), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(allVideos);

        // 24시간 전 데이터 (growth rate 계산용) - 이전 데이터 없음
        given(trendingVideoRepository.findByVideoIdInAndCollectedAtBetween(
                anyList(), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(List.of());

        // 채널 정보
        Channel ch1 = Channel.builder()
                .channelId("UC_ch1").title("TopChannel").thumbnailUrl("https://img.com/ch1.jpg")
                .subscriberCount(100_000L).videoCount(200L).totalViewCount(50_000_000L).build();
        Channel ch2 = Channel.builder()
                .channelId("UC_ch2").title("MidChannel").thumbnailUrl("https://img.com/ch2.jpg")
                .subscriberCount(50_000L).videoCount(100L).totalViewCount(10_000_000L).build();
        Channel ch3 = Channel.builder()
                .channelId("UC_ch3").title("LowChannel").thumbnailUrl("https://img.com/ch3.jpg")
                .subscriberCount(10_000L).videoCount(30L).totalViewCount(1_000_000L).build();

        given(channelRepository.findByChannelIdIn(channelIds))
                .willReturn(List.of(ch1, ch2, ch3));

        // When
        List<ChannelRankingResponse> result = channelRankingService.getRanking("KR", 10);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getChannelId()).isEqualTo("UC_ch1");
        assertThat(result.get(0).getSurgeScore()).isGreaterThan(result.get(1).getSurgeScore());
        assertThat(result.get(1).getSurgeScore()).isGreaterThan(result.get(2).getSurgeScore());
        assertThat(result.get(0).getTrendingVideoCount()).isEqualTo(2);
        assertThat(result.get(1).getTrendingVideoCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("surgeScore에 따라 올바른 등급(S/A/B/C/D) 부여")
    void getRanking_assignsCorrectGrades() {
        // Given
        // Min-Max 정규화 특성상, 각 지표별로 채널들이 고르게 분포해야 등급이 잘 나뉨
        // 설계: 모든 채널이 동일 구독자(10,000)로 burst_ratio만 views에 비례하도록 통제
        // 이전 데이터 없음(growth=0 동일) → 4개 지표(freq, rank, engage, burst)로 등급 결정
        // max 가능 점수: 0.25 + 0.20 + 0 + 0.15 + 0.20 = 0.80 (growth_rate 모두 동일 → norm=0)
        // 따라서 등급 매칭을 점수 분포에 맞게 검증

        List<String> channelIds = List.of("UC_top", "UC_high", "UC_mid", "UC_low", "UC_bot");

        given(trendingVideoRepository.findDistinctChannelIdsByCountryCode(
                eq("KR"), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(channelIds);

        // top: 5개 영상, rank 1~5, 고조회/고참여 → 모든 지표 최고
        TrendingVideo topV1 = TrendingVideo.builder()
                .videoId("t1").channelId("UC_top").channelTitle("Top")
                .countryCode("KR").rankPosition(1)
                .viewCount(1_000_000L).likeCount(50_000L).commentCount(10_000L)
                .collectedAt(NOW).build();
        TrendingVideo topV2 = TrendingVideo.builder()
                .videoId("t2").channelId("UC_top").channelTitle("Top")
                .countryCode("KR").rankPosition(2)
                .viewCount(900_000L).likeCount(45_000L).commentCount(9_000L)
                .collectedAt(NOW).build();
        TrendingVideo topV3 = TrendingVideo.builder()
                .videoId("t3").channelId("UC_top").channelTitle("Top")
                .countryCode("KR").rankPosition(3)
                .viewCount(800_000L).likeCount(40_000L).commentCount(8_000L)
                .collectedAt(NOW).build();
        TrendingVideo topV4 = TrendingVideo.builder()
                .videoId("t4").channelId("UC_top").channelTitle("Top")
                .countryCode("KR").rankPosition(4)
                .viewCount(700_000L).likeCount(35_000L).commentCount(7_000L)
                .collectedAt(NOW).build();
        TrendingVideo topV5 = TrendingVideo.builder()
                .videoId("t5").channelId("UC_top").channelTitle("Top")
                .countryCode("KR").rankPosition(5)
                .viewCount(600_000L).likeCount(30_000L).commentCount(6_000L)
                .collectedAt(NOW).build();

        // high: 4개 영상, rank 8~14
        TrendingVideo highV1 = TrendingVideo.builder()
                .videoId("h1").channelId("UC_high").channelTitle("High")
                .countryCode("KR").rankPosition(8)
                .viewCount(700_000L).likeCount(28_000L).commentCount(5_600L)
                .collectedAt(NOW).build();
        TrendingVideo highV2 = TrendingVideo.builder()
                .videoId("h2").channelId("UC_high").channelTitle("High")
                .countryCode("KR").rankPosition(10)
                .viewCount(600_000L).likeCount(24_000L).commentCount(4_800L)
                .collectedAt(NOW).build();
        TrendingVideo highV3 = TrendingVideo.builder()
                .videoId("h3").channelId("UC_high").channelTitle("High")
                .countryCode("KR").rankPosition(12)
                .viewCount(500_000L).likeCount(20_000L).commentCount(4_000L)
                .collectedAt(NOW).build();
        TrendingVideo highV4 = TrendingVideo.builder()
                .videoId("h4").channelId("UC_high").channelTitle("High")
                .countryCode("KR").rankPosition(14)
                .viewCount(400_000L).likeCount(16_000L).commentCount(3_200L)
                .collectedAt(NOW).build();

        // mid: 3개 영상, rank 20~30
        TrendingVideo midV1 = TrendingVideo.builder()
                .videoId("m1").channelId("UC_mid").channelTitle("Mid")
                .countryCode("KR").rankPosition(20)
                .viewCount(300_000L).likeCount(9_000L).commentCount(1_500L)
                .collectedAt(NOW).build();
        TrendingVideo midV2 = TrendingVideo.builder()
                .videoId("m2").channelId("UC_mid").channelTitle("Mid")
                .countryCode("KR").rankPosition(25)
                .viewCount(250_000L).likeCount(7_500L).commentCount(1_250L)
                .collectedAt(NOW).build();
        TrendingVideo midV3 = TrendingVideo.builder()
                .videoId("m3").channelId("UC_mid").channelTitle("Mid")
                .countryCode("KR").rankPosition(30)
                .viewCount(200_000L).likeCount(6_000L).commentCount(1_000L)
                .collectedAt(NOW).build();

        // low: 2개 영상, rank 35~40
        TrendingVideo lowV1 = TrendingVideo.builder()
                .videoId("l1").channelId("UC_low").channelTitle("Low")
                .countryCode("KR").rankPosition(35)
                .viewCount(120_000L).likeCount(2_400L).commentCount(360L)
                .collectedAt(NOW).build();
        TrendingVideo lowV2 = TrendingVideo.builder()
                .videoId("l2").channelId("UC_low").channelTitle("Low")
                .countryCode("KR").rankPosition(40)
                .viewCount(80_000L).likeCount(1_600L).commentCount(240L)
                .collectedAt(NOW).build();

        // bot: 1개 영상, rank 50, 최저 지표
        TrendingVideo botV1 = TrendingVideo.builder()
                .videoId("b1").channelId("UC_bot").channelTitle("Bot")
                .countryCode("KR").rankPosition(50)
                .viewCount(10_000L).likeCount(100L).commentCount(10L)
                .collectedAt(NOW).build();

        List<TrendingVideo> allVideos = List.of(
                topV1, topV2, topV3, topV4, topV5,
                highV1, highV2, highV3, highV4,
                midV1, midV2, midV3,
                lowV1, lowV2,
                botV1
        );

        given(trendingVideoRepository.findByChannelIdInAndCollectedAtBetween(
                eq(channelIds), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(allVideos);

        given(trendingVideoRepository.findByVideoIdInAndCollectedAtBetween(
                anyList(), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(List.of());

        // 모든 채널 동일 구독자 (burst_ratio가 views에 비례하도록)
        Channel chTop = Channel.builder()
                .channelId("UC_top").title("Top")
                .subscriberCount(10_000L).videoCount(100L).totalViewCount(50_000_000L).build();
        Channel chHigh = Channel.builder()
                .channelId("UC_high").title("High")
                .subscriberCount(10_000L).videoCount(80L).totalViewCount(30_000_000L).build();
        Channel chMid = Channel.builder()
                .channelId("UC_mid").title("Mid")
                .subscriberCount(10_000L).videoCount(60L).totalViewCount(15_000_000L).build();
        Channel chLow = Channel.builder()
                .channelId("UC_low").title("Low")
                .subscriberCount(10_000L).videoCount(40L).totalViewCount(5_000_000L).build();
        Channel chBot = Channel.builder()
                .channelId("UC_bot").title("Bot")
                .subscriberCount(10_000L).videoCount(20L).totalViewCount(500_000L).build();

        given(channelRepository.findByChannelIdIn(channelIds))
                .willReturn(List.of(chTop, chHigh, chMid, chLow, chBot));

        // When
        List<ChannelRankingResponse> result = channelRankingService.getRanking("KR", 10);

        // Then
        assertThat(result).hasSize(5);

        // 점수 내림차순 정렬 확인
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getSurgeScore())
                    .isGreaterThanOrEqualTo(result.get(i + 1).getSurgeScore());
        }

        // 등급 라벨 매핑 검증 (등급별 올바른 라벨 지정)
        for (ChannelRankingResponse r : result) {
            String expectedLabel = switch (r.getGrade()) {
                case "S" -> "지금 가장 핫한 채널!";
                case "A" -> "주목할 만한 성장세!";
                case "B" -> "꾸준히 성장하는 채널";
                case "C" -> "잠재력 있는 채널";
                default -> "트렌딩에 등장한 채널";
            };
            assertThat(r.getGradeLabel()).isEqualTo(expectedLabel);
        }

        // 1위 채널이 최고 등급, 최하위 채널이 최저 등급
        String topGrade = result.get(0).getGrade();
        String botGrade = result.get(4).getGrade();
        List<String> gradeOrder = List.of("S", "A", "B", "C", "D");
        assertThat(gradeOrder.indexOf(topGrade)).isLessThan(gradeOrder.indexOf(botGrade));
    }

    @Test
    @DisplayName("구독자 대비 조회수 높은 채널 darkhorse 감지")
    void getRanking_detectsDarkhorse() {
        // Given
        List<String> channelIds = List.of("UC_small", "UC_big");

        given(trendingVideoRepository.findDistinctChannelIdsByCountryCode(
                eq("KR"), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(channelIds);

        // 소규모 채널: 구독자 1,000 but 조회수 50,000 → burst_ratio = 50 (>= 10)
        TrendingVideo smallVid = TrendingVideo.builder()
                .videoId("sv1").channelId("UC_small").channelTitle("SmallChannel")
                .countryCode("KR").rankPosition(5)
                .viewCount(50_000L).likeCount(5_000L).commentCount(500L)
                .collectedAt(NOW).build();

        // 대규모 채널: 구독자 10,000,000 but 조회수 5,000,000 → burst_ratio = 0.5 (< 10)
        TrendingVideo bigVid = TrendingVideo.builder()
                .videoId("bv1").channelId("UC_big").channelTitle("BigChannel")
                .countryCode("KR").rankPosition(3)
                .viewCount(5_000_000L).likeCount(100_000L).commentCount(10_000L)
                .collectedAt(NOW).build();

        given(trendingVideoRepository.findByChannelIdInAndCollectedAtBetween(
                eq(channelIds), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(List.of(smallVid, bigVid));

        given(trendingVideoRepository.findByVideoIdInAndCollectedAtBetween(
                anyList(), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(List.of());

        Channel smallCh = Channel.builder()
                .channelId("UC_small").title("SmallChannel")
                .subscriberCount(1_000L).videoCount(10L).totalViewCount(100_000L).build();
        Channel bigCh = Channel.builder()
                .channelId("UC_big").title("BigChannel")
                .subscriberCount(10_000_000L).videoCount(500L).totalViewCount(500_000_000L).build();

        given(channelRepository.findByChannelIdIn(channelIds))
                .willReturn(List.of(smallCh, bigCh));

        // When
        List<ChannelRankingResponse> result = channelRankingService.getRanking("KR", 10);

        // Then
        assertThat(result).hasSize(2);

        ChannelRankingResponse smallResult = result.stream()
                .filter(r -> r.getChannelId().equals("UC_small"))
                .findFirst().orElseThrow();
        ChannelRankingResponse bigResult = result.stream()
                .filter(r -> r.getChannelId().equals("UC_big"))
                .findFirst().orElseThrow();

        assertThat(smallResult.isDarkhorse()).isTrue();
        assertThat(smallResult.getBurstRatio()).isGreaterThanOrEqualTo(10.0);

        assertThat(bigResult.isDarkhorse()).isFalse();
        assertThat(bigResult.getBurstRatio()).isLessThan(10.0);
    }

    @Test
    @DisplayName("트렌딩 데이터가 없으면 빈 리스트 반환")
    void getRanking_withNoData_returnsEmptyList() {
        // Given
        given(trendingVideoRepository.findDistinctChannelIdsByCountryCode(
                eq("KR"), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(List.of());

        // When
        List<ChannelRankingResponse> result = channelRankingService.getRanking("KR", 10);

        // Then
        assertThat(result).isEmpty();
    }
}
