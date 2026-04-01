CREATE TABLE channels (
    channel_id       VARCHAR(30) PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    thumbnail_url    VARCHAR(500),
    subscriber_count BIGINT DEFAULT 0,
    video_count      BIGINT DEFAULT 0,
    total_view_count BIGINT DEFAULT 0,
    first_seen_at    TIMESTAMP WITH TIME ZONE,
    updated_at       TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_channels_updated ON channels(updated_at DESC);

CREATE TABLE channel_snapshots (
    id                   BIGSERIAL PRIMARY KEY,
    channel_id           VARCHAR(30) NOT NULL REFERENCES channels(channel_id),
    subscriber_count     BIGINT DEFAULT 0,
    video_count          BIGINT DEFAULT 0,
    total_view_count     BIGINT DEFAULT 0,
    trending_video_count INT DEFAULT 0,
    snapshot_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_channel_snapshots_channel_time
    ON channel_snapshots(channel_id, snapshot_at DESC);

ALTER TABLE trending_videos ADD COLUMN channel_id VARCHAR(30);
CREATE INDEX idx_trending_channel_id ON trending_videos(channel_id);
