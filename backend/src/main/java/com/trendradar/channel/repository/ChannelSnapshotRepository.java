package com.trendradar.channel.repository;

import com.trendradar.channel.domain.ChannelSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ChannelSnapshotRepository extends JpaRepository<ChannelSnapshot, Long> {

    List<ChannelSnapshot> findByChannelIdAndSnapshotAtBetweenOrderBySnapshotAtDesc(
            String channelId, OffsetDateTime from, OffsetDateTime to);

    List<ChannelSnapshot> findByChannelIdOrderBySnapshotAtDesc(String channelId);
}
