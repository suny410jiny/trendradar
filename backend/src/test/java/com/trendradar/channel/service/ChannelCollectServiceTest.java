package com.trendradar.channel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trendradar.channel.domain.Channel;
import com.trendradar.channel.domain.ChannelSnapshot;
import com.trendradar.channel.repository.ChannelRepository;
import com.trendradar.channel.repository.ChannelSnapshotRepository;
import com.trendradar.youtube.client.YouTubeApiClient;
import com.trendradar.youtube.dto.YouTubeChannelResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("채널 수집 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class ChannelCollectServiceTest {

    @Mock
    private YouTubeApiClient youTubeApiClient;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelSnapshotRepository channelSnapshotRepository;

    @InjectMocks
    private ChannelCollectService channelCollectService;

    private static final OffsetDateTime COLLECTED_AT = OffsetDateTime.of(2026, 4, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    @DisplayName("새 채널 수집 시 Channel과 ChannelSnapshot 저장")
    void collectChannels_whenNewChannels_createsChannelAndSnapshot() {
        // Given
        List<String> channelIds = List.of("UC_channel1");
        YouTubeChannelResponse response = createMockChannelResponse(
                "UC_channel1", "테스트 채널", 100000L, 500L, 50000000L);

        given(youTubeApiClient.fetchChannels(channelIds)).willReturn(response);
        given(channelRepository.findById("UC_channel1")).willReturn(Optional.empty());
        given(channelRepository.save(any(Channel.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        channelCollectService.collectChannels(channelIds, COLLECTED_AT);

        // Then
        ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
        verify(channelRepository).save(channelCaptor.capture());
        Channel savedChannel = channelCaptor.getValue();
        assertThat(savedChannel.getChannelId()).isEqualTo("UC_channel1");
        assertThat(savedChannel.getTitle()).isEqualTo("테스트 채널");
        assertThat(savedChannel.getSubscriberCount()).isEqualTo(100000L);
        assertThat(savedChannel.getVideoCount()).isEqualTo(500L);
        assertThat(savedChannel.getTotalViewCount()).isEqualTo(50000000L);

        ArgumentCaptor<ChannelSnapshot> snapshotCaptor = ArgumentCaptor.forClass(ChannelSnapshot.class);
        verify(channelSnapshotRepository).save(snapshotCaptor.capture());
        ChannelSnapshot savedSnapshot = snapshotCaptor.getValue();
        assertThat(savedSnapshot.getChannelId()).isEqualTo("UC_channel1");
        assertThat(savedSnapshot.getSubscriberCount()).isEqualTo(100000L);
        assertThat(savedSnapshot.getSnapshotAt()).isEqualTo(COLLECTED_AT);
    }

    @Test
    @DisplayName("기존 채널 수집 시 stats 업데이트 + 스냅샷 저장")
    void collectChannels_whenExistingChannel_updatesStats() {
        // Given
        List<String> channelIds = List.of("UC_existing");
        YouTubeChannelResponse response = createMockChannelResponse(
                "UC_existing", "업데이트 채널", 200000L, 600L, 80000000L);

        Channel existingChannel = Channel.builder()
                .channelId("UC_existing")
                .title("이전 채널명")
                .subscriberCount(150000L)
                .videoCount(550L)
                .totalViewCount(60000000L)
                .build();

        given(youTubeApiClient.fetchChannels(channelIds)).willReturn(response);
        given(channelRepository.findById("UC_existing")).willReturn(Optional.of(existingChannel));

        // When
        channelCollectService.collectChannels(channelIds, COLLECTED_AT);

        // Then - updateStats가 호출되어 값이 변경됨 (JPA dirty checking)
        assertThat(existingChannel.getTitle()).isEqualTo("업데이트 채널");
        assertThat(existingChannel.getSubscriberCount()).isEqualTo(200000L);
        assertThat(existingChannel.getVideoCount()).isEqualTo(600L);
        assertThat(existingChannel.getTotalViewCount()).isEqualTo(80000000L);

        // save()는 호출되지 않아야 함 (dirty checking)
        verify(channelRepository, never()).save(any(Channel.class));

        // 스냅샷은 저장되어야 함
        ArgumentCaptor<ChannelSnapshot> snapshotCaptor = ArgumentCaptor.forClass(ChannelSnapshot.class);
        verify(channelSnapshotRepository).save(snapshotCaptor.capture());
        ChannelSnapshot savedSnapshot = snapshotCaptor.getValue();
        assertThat(savedSnapshot.getChannelId()).isEqualTo("UC_existing");
        assertThat(savedSnapshot.getSubscriberCount()).isEqualTo(200000L);
    }

    @Test
    @DisplayName("빈 채널 ID 목록이면 API 호출하지 않음")
    void collectChannels_whenEmptyList_doesNothing() {
        // Given
        List<String> emptyList = Collections.emptyList();

        // When
        channelCollectService.collectChannels(emptyList, COLLECTED_AT);

        // Then
        verify(youTubeApiClient, never()).fetchChannels(anyList());
        verify(channelRepository, never()).save(any(Channel.class));
        verify(channelSnapshotRepository, never()).save(any(ChannelSnapshot.class));
    }

    @Test
    @DisplayName("60개 채널 수집 시 2번 배치 호출 (50 + 10)")
    void collectChannels_batchesOver50Channels() {
        // Given
        List<String> channelIds = IntStream.rangeClosed(1, 60)
                .mapToObj(i -> "UC_channel" + i)
                .toList();

        // 첫 번째 배치 (50개) 응답
        YouTubeChannelResponse firstBatchResponse = createMockChannelResponse(
                "UC_channel1", "채널1", 1000L, 10L, 10000L);
        // 두 번째 배치 (10개) 응답
        YouTubeChannelResponse secondBatchResponse = createMockChannelResponse(
                "UC_channel51", "채널51", 2000L, 20L, 20000L);

        given(youTubeApiClient.fetchChannels(argThat(list -> list != null && list.size() == 50)))
                .willReturn(firstBatchResponse);
        given(youTubeApiClient.fetchChannels(argThat(list -> list != null && list.size() == 10)))
                .willReturn(secondBatchResponse);
        given(channelRepository.findById(anyString())).willReturn(Optional.empty());
        given(channelRepository.save(any(Channel.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        channelCollectService.collectChannels(channelIds, COLLECTED_AT);

        // Then - fetchChannels가 2번 호출되어야 함
        verify(youTubeApiClient, times(2)).fetchChannels(anyList());

        // 첫 번째 배치는 50개
        ArgumentCaptor<List<String>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(youTubeApiClient, times(2)).fetchChannels(batchCaptor.capture());
        List<List<String>> batches = batchCaptor.getAllValues();
        assertThat(batches.get(0)).hasSize(50);
        assertThat(batches.get(1)).hasSize(10);
    }

    private YouTubeChannelResponse createMockChannelResponse(String id, String title,
                                                              long subscribers, long videos, long views) {
        String json = String.format("""
            {"items":[{"id":"%s","snippet":{"title":"%s","thumbnails":{"high":{"url":"https://img.com/thumb.jpg"}}},\
            "statistics":{"subscriberCount":"%d","videoCount":"%d","viewCount":"%d"}}]}
            """, id, title, subscribers, videos, views);
        try {
            return new ObjectMapper().readValue(json, YouTubeChannelResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
