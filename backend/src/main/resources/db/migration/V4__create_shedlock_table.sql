-- 무중단 배포 환경에서 스케줄러 중복 실행 방지를 위한 ShedLock 락 테이블

create table shedlock (
    name varchar(64) not null,
    lock_until timestamp(3) not null,
    locked_at timestamp(3) not null,
    locked_by varchar(255) not null,
    primary key (name)
) engine=InnoDB;
