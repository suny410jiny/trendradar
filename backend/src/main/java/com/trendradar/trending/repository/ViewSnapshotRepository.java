package com.trendradar.trending.repository;

import com.trendradar.trending.domain.ViewSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ViewSnapshotRepository extends JpaRepository<ViewSnapshot, Long> {

    List<ViewSnapshot> findByVideoIdAndSnapshotAtAfterOrderBySnapshotAtAsc(
            String videoId, OffsetDateTime after);

    List<ViewSnapshot> findByVideoIdIn(List<String> videoIds);
}
