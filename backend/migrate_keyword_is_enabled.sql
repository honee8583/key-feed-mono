-- Issue #50: 구독 만료 시 초과 키워드 비활성화 지원을 위한 컬럼 추가
-- ddl-auto: validate 환경에서는 이 스크립트를 수동으로 실행해야 합니다.
ALTER TABLE keyword ADD COLUMN is_enabled BOOLEAN NOT NULL DEFAULT TRUE;
