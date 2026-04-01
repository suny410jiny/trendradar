CREATE TABLE ai_analyses (
    id            BIGSERIAL PRIMARY KEY,
    analysis_type VARCHAR(30) NOT NULL,
    target_id     VARCHAR(30) NOT NULL,
    country_code  VARCHAR(5),
    content       TEXT NOT NULL,
    model_used    VARCHAR(50),
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ai_analyses_lookup
    ON ai_analyses(analysis_type, target_id, created_at DESC);
