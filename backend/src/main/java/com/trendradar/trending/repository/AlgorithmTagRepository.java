package com.trendradar.trending.repository;

import com.trendradar.trending.domain.AlgorithmTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlgorithmTagRepository extends JpaRepository<AlgorithmTag, Long> {

    List<AlgorithmTag> findByVideoId(String videoId);

    List<AlgorithmTag> findByVideoIdIn(List<String> videoIds);
}
