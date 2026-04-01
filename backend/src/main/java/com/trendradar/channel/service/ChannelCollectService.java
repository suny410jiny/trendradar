package com.trendradar.channel.service;

import com.trendradar.channel.domain.Channel;
import com.trendradar.channel.domain.ChannelSnapshot;
import com.trendradar.channel.repository.ChannelRepository;
import com.trendradar.channel.repository.ChannelSnapshotRepository;
import com.trendradar.youtube.client.YouTubeApiClient;
import com.trendradar.youtube.dto.YouTubeChannelItem;
import com.trendradar.youtube.dto.YouTubeChannelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelCollectService {

    private static final int BATCH_SIZE = 50;

    private final YouTubeApiClient youTubeApiClient;
    private final ChannelRepository channelRepository;
    private final ChannelSnapshotRepository channelSnapshotRepository;

    @Transactional
    public void collectChannels(List<String> channelIds, OffsetDateTime collectedAt) {
        if (channelIds == null || channelIds.isEmpty()) {
            log.info("No channel IDs to collect, skipping");
            return;
        }

        log.info("Collecting {} channels", channelIds.size());

        for (int i = 0; i < channelIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, channelIds.size());
            List<String> batch = channelIds.subList(i, end);

            log.info("Fetching channel batch {}/{}, size={}",
                    (i / BATCH_SIZE) + 1,
                    (int) Math.ceil((double) channelIds.size() / BATCH_SIZE),
                    batch.size());

            YouTubeChannelResponse response = youTubeApiClient.fetchChannels(batch);
            if (response == null || response.getItems() == null) {
                log.warn("No channel data returned for batch starting at index={}", i);
                continue;
            }

            for (YouTubeChannelItem item : response.getItems()) {
                processChannelItem(item, collectedAt);
            }
        }

        log.info("Channel collection completed, total={}", channelIds.size());
    }

    private void processChannelItem(YouTubeChannelItem item, OffsetDateTime collectedAt) {
        String channelId = item.getId();
        String title = item.getSnippet() != null ? item.getSnippet().getTitle() : "";
        String thumbnailUrl = extractThumbnailUrl(item);
        Long subscriberCount = parseLong(item.getStatistics() != null ? item.getStatistics().getSubscriberCount() : null);
        Long videoCount = parseLong(item.getStatistics() != null ? item.getStatistics().getVideoCount() : null);
        Long totalViewCount = parseLong(item.getStatistics() != null ? item.getStatistics().getViewCount() : null);

        Optional<Channel> existingChannel = channelRepository.findById(channelId);

        if (existingChannel.isPresent()) {
            Channel channel = existingChannel.get();
            channel.updateStats(title, thumbnailUrl, subscriberCount, videoCount, totalViewCount);
            log.debug("Updated existing channel={}", channelId);
        } else {
            Channel newChannel = Channel.builder()
                    .channelId(channelId)
                    .title(title)
                    .thumbnailUrl(thumbnailUrl)
                    .subscriberCount(subscriberCount)
                    .videoCount(videoCount)
                    .totalViewCount(totalViewCount)
                    .firstSeenAt(collectedAt)
                    .build();
            channelRepository.save(newChannel);
            log.debug("Created new channel={}", channelId);
        }

        ChannelSnapshot snapshot = ChannelSnapshot.builder()
                .channelId(channelId)
                .subscriberCount(subscriberCount)
                .videoCount(videoCount)
                .totalViewCount(totalViewCount)
                .snapshotAt(collectedAt)
                .build();
        channelSnapshotRepository.save(snapshot);
    }

    private String extractThumbnailUrl(YouTubeChannelItem item) {
        if (item.getSnippet() == null || item.getSnippet().getThumbnails() == null
                || item.getSnippet().getThumbnails().getHigh() == null) {
            return null;
        }
        return item.getSnippet().getThumbnails().getHigh().getUrl();
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
