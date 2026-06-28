-- 소스명 관리 주체를 user_source.user_defined_name 에서 source.name 으로 이전한다.
-- (이슈 #79) 사용자가 소스를 등록하는 구조에서 관리자가 등록하는 구조로 전환.

-- 1. source 에 name 컬럼 추가 (우선 nullable 로 추가 후 백필)
alter table source
    add column name varchar(100);

-- 2. 백필: 한 source 를 여러 사용자가 서로 다른 이름으로 등록했을 수 있으므로
--    충돌을 피하기 위해 가장 먼저 등록된(user_source_id 최솟값) user_defined_name 을 대표값으로 사용한다.
update source s
set s.name = (
    select us.user_defined_name
    from user_source us
    where us.source_id = s.source_id
    order by us.user_source_id asc
    limit 1
)
where exists (
    select 1 from user_source us where us.source_id = s.source_id
);

-- 3. 구독자가 없어 백필되지 못한 source 는 url 을 임시 이름으로 채운다 (관리자가 추후 보정).
update source
set name = url
where name is null;

-- 4. NOT NULL 제약 적용
alter table source
    modify column name varchar(100) not null;

-- 5. user_source 에서 user_defined_name 컬럼 제거
alter table user_source
    drop column user_defined_name;
