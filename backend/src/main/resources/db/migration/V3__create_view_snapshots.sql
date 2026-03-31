CREATE TABLE view_snapshots (
    id          BIGSERIAL PRIMARY KEY,
    video_id    VARCHAR(20) NOT NULL,
    view_count  BIGINT NOT NULL,
    snapshot_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_snapshots_video_id
    ON view_snapshots(video_id, snapshot_at DESC);
