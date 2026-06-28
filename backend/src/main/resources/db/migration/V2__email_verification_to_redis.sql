-- 이메일 인증 코드 저장소를 Redis로 전환 (#71)
-- 회원가입 인증 완료(영구 상태)는 user.email_verified 로 이전,
-- 비밀번호 재설정 등 인증 이력은 password_reset_audit 로 보존,
-- 휘발성 인증 코드 테이블(email_verification)은 제거한다.

-- 1) 회원가입 인증 완료 영구 플래그
ALTER TABLE `user` ADD COLUMN email_verified bit not null default b'0';

-- 기존 가입 사용자 회귀 방지가 필요하면 아래 백필을 활성화한다(운영 정책 결정).
-- 미백필 시 기존 사용자는 로그인 전 이메일 재인증이 필요하다.
-- UPDATE `user` SET email_verified = b'1';

-- 2) 비밀번호 재설정 인증 이력 (감사 로그)
create table password_reset_audit (
    id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    email varchar(120) not null,
    type enum ('CODE_VERIFIED','PASSWORD_RESET') not null,
    primary key (id)
) engine=InnoDB;

-- 3) 휘발성 인증 코드 테이블 제거 (Redis TTL로 대체)
drop table email_verification;
