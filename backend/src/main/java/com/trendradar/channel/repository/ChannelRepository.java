package com.trendradar.channel.repository;

import com.trendradar.channel.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, String> {

    List<Channel> findByChannelIdIn(List<String> channelIds);
}
