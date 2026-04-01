package com.trendradar.trending.repository;

import com.trendradar.trending.domain.AlgorithmTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlgorithmTagRepository extends JpaRepository<AlgorithmTag, Long> {

    List<AlgorithmTag> findByVideoId(String videoId);

    List<AlgorithmTag> findByVideoIdIn(List<String> videoIds);

    void deleteByVideoIdIn(List<String> videoIds);

    @Query("SELECT a.videoId FROM AlgorithmTag a WHERE a.videoId IN :videoIds AND a.tagType = :tagType")
    List<String> findVideoIdsByTagType(@Param("videoIds") List<String> videoIds, @Param("tagType") String tagType);
}
