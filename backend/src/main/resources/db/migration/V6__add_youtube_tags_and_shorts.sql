-- YouTube 영상의 실제 태그/키워드 저장 컬럼 추가
-- snippet.tags 배열을 쉼표 구분 문자열로 저장
ALTER TABLE trending_videos ADD COLUMN youtube_tags TEXT;

-- Shorts 여부 판별 컬럼 (duration 60초 이하)
ALTER TABLE trending_videos ADD COLUMN is_short BOOLEAN DEFAULT FALSE;

-- 인덱스: 키워드 검색용 (Full-text 검색 대비)
CREATE INDEX idx_trending_videos_youtube_tags ON trending_videos USING gin(to_tsvector('simple', COALESCE(youtube_tags, '')));

-- 인덱스: Shorts 필터용
CREATE INDEX idx_trending_videos_is_short ON trending_videos (is_short);
