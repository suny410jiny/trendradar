CREATE TABLE trending_videos (
    id              BIGSERIAL PRIMARY KEY,
    video_id        VARCHAR(20) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    channel_title   VARCHAR(200),
    country_code    VARCHAR(5) NOT NULL,
    category_id     INTEGER,
    rank_position   INTEGER NOT NULL,
    view_count      BIGINT DEFAULT 0,
    like_count      BIGINT DEFAULT 0,
    comment_count   BIGINT DEFAULT 0,
    published_at    TIMESTAMP WITH TIME ZONE,
    thumbnail_url   VARCHAR(500),
    duration        VARCHAR(20),
    collected_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_trending_country_collected
    ON trending_videos(country_code, collected_at DESC);
CREATE INDEX idx_trending_video_id
    ON trending_videos(video_id);
