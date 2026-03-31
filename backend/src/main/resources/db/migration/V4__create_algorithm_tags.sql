CREATE TABLE algorithm_tags (
    id              BIGSERIAL PRIMARY KEY,
    video_id        VARCHAR(20) NOT NULL,
    tag_type        VARCHAR(50) NOT NULL,
    calculated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_tags_video_id ON algorithm_tags(video_id);
