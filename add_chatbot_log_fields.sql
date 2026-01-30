-- chatbot_log 테이블에 성능 평가용 필드 추가
-- MySQL Workbench 또는 터미널에서 실행하세요
USE silverlink;

ALTER TABLE silverlink.chatbot_logs
ADD COLUMN response_time_ms INT NULL COMMENT '전체 응답시간(ms)',
ADD COLUMN embedding_time_ms INT NULL COMMENT '임베딩 시간(ms)',
ADD COLUMN search_time_ms INT NULL COMMENT '검색 시간(ms)',
ADD COLUMN llm_time_ms INT NULL COMMENT 'LLM 응답시간(ms)',
ADD COLUMN model_name VARCHAR(50) NULL COMMENT '사용된 모델명';

-- 확인
DESCRIBE silverlink.chatbot_logs;
