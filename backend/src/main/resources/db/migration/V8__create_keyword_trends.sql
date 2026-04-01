CREATE TABLE keyword_trends (
    id             BIGSERIAL PRIMARY KEY,
    keyword        VARCHAR(200) NOT NULL,
    country_code   VARCHAR(5) NOT NULL,
    video_count    INT DEFAULT 0,
    total_views    BIGINT DEFAULT 0,
    avg_engagement DOUBLE PRECISION DEFAULT 0,
    period_type    VARCHAR(10) NOT NULL,
    period_start   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_keyword_trends_lookup
    ON keyword_trends(keyword, country_code, period_type);
CREATE UNIQUE INDEX idx_keyword_trends_unique
    ON keyword_trends(keyword, country_code, period_type, period_start);
