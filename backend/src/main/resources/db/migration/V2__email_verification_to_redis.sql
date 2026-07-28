ALTER TABLE `user` ADD COLUMN email_verified bit not null default b'0';

create table password_reset_audit (
    id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    email varchar(120) not null,
    type enum ('CODE_VERIFIED','PASSWORD_RESET') not null,
    primary key (id)
) engine=InnoDB;

drop table email_verification;
