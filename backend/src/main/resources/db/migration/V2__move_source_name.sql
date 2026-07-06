-- 소스명 관리 주체를 UserSource.user_defined_name 에서 Source.name 으로 이전
-- 이슈 #79

-- 1. source 테이블에 name 컬럼 추가 (우선 NULL 허용)
ALTER TABLE source ADD COLUMN name varchar(100) NULL;

-- 2. 임시 백필 (운영 데이터 없음 전제 - 관리자가 후속으로 재명명)
--    url 이 100자를 초과할 수 있으므로 앞 100자로 자른다.
UPDATE source SET name = SUBSTRING(url, 1, 100) WHERE name IS NULL;

-- 3. NOT NULL 제약 적용
ALTER TABLE source MODIFY COLUMN name varchar(100) NOT NULL;

-- 4. user_source 에서 소스명 컬럼 제거
ALTER TABLE user_source DROP COLUMN user_defined_name;
