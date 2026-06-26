create table `user` (
    user_id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    customer_key varchar(200),
    email varchar(120),
    is_social bit not null,
    is_withdraw bit not null,
    last_notification_checked_at datetime(6),
    password varchar(255),
    role enum ('ADMIN','USER'),
    setting_push bit not null,
    sns_type varchar(255),
    username varchar(60),
    primary key (user_id)
) engine=InnoDB;

create table source (
    source_id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    last_crawled_at datetime(6),
    last_item_hash varchar(255),
    logo_url varchar(2048),
    url varchar(767) not null,
    primary key (source_id),
    constraint uk_source_url unique (url)
) engine=InnoDB;

create table content (
    content_id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    original_url varchar(2048),
    published_at datetime(6) not null,
    source_id bigint not null,
    source_name varchar(255),
    summary text not null,
    thumbnail_url varchar(2048),
    title varchar(512) not null,
    primary key (content_id)
) engine=InnoDB;

create table email_verification (
    id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    attempt_count integer,
    code varchar(6),
    email varchar(255),
    expires_at datetime(6),
    locked_until datetime(6),
    purpose enum ('CHANGE','RESET','SIGNUP'),
    status enum ('EXPIRED','LOCKED','PENDING','VERIFIED'),
    primary key (id)
) engine=InnoDB;

create table notification (
    notification_id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    content_id bigint,
    is_read bit not null,
    message varchar(255),
    title varchar(255),
    original_url varchar(255),
    user_id bigint,
    primary key (notification_id)
) engine=InnoDB;

create table notification_processed_contents (
    content_id bigint not null,
    processed_at datetime(6) not null,
    primary key (content_id)
) engine=InnoDB;

create table outbox (
    id bigint not null auto_increment,
    aggregate_id bigint not null,
    aggregate_type varchar(255) not null,
    created_at datetime(6) not null,
    error_message text,
    event_type varchar(255) not null,
    idempotency_key varchar(255),
    last_tried_at datetime(6),
    max_retry integer not null,
    next_retry_at datetime(6),
    payload json not null,
    published_at datetime(6),
    retry_count integer not null,
    status enum ('FAILED','PENDING','PUBLISHED') not null,
    primary key (id),
    constraint uk_idempotency_key unique (idempotency_key)
) engine=InnoDB;

create table bookmark_folder (
    bookmark_folder_id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    color varchar(20),
    icon varchar(50),
    name varchar(100) not null,
    user_id bigint not null,
    primary key (bookmark_folder_id),
    constraint uk_folder_user_name unique (user_id, name),
    constraint fk_bookmark_folder_user foreign key (user_id) references `user` (user_id)
) engine=InnoDB;

create table bookmark (
    bookmark_id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    content_id varchar(255) not null,
    bookmark_folder_id bigint,
    user_id bigint not null,
    primary key (bookmark_id),
    constraint uk_bookmark_user_content unique (user_id, content_id),
    constraint fk_bookmark_user foreign key (user_id) references `user` (user_id),
    constraint fk_bookmark_folder foreign key (bookmark_folder_id) references bookmark_folder (bookmark_folder_id)
) engine=InnoDB;

create table keyword (
    keyword_id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    is_enabled bit not null,
    is_notification_enabled bit not null,
    name varchar(255),
    user_id bigint,
    primary key (keyword_id),
    constraint fk_keyword_user foreign key (user_id) references `user` (user_id)
) engine=InnoDB;

create table payment_method (
    method_id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    billing_key varchar(300) not null,
    deleted_at datetime(6),
    display_number varchar(100),
    is_active bit not null,
    is_default bit not null,
    method_type enum ('BANK','CARD') not null,
    provider_name varchar(100),
    user_id bigint not null,
    primary key (method_id),
    constraint fk_payment_method_user foreign key (user_id) references `user` (user_id)
) engine=InnoDB;

create table user_source (
    user_source_id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    receive_feed bit not null,
    user_defined_name varchar(100) not null,
    source_id bigint not null,
    user_id bigint not null,
    primary key (user_source_id),
    constraint fk_user_source_user foreign key (user_id) references `user` (user_id),
    constraint fk_user_source_source foreign key (source_id) references source (source_id)
) engine=InnoDB;

create table subscription (
    subscription_id bigint not null auto_increment,
    created_at datetime(6),
    updated_at datetime(6),
    canceled_at datetime(6),
    expired_at datetime(6),
    next_billing_at datetime(6),
    order_name varchar(255) not null,
    price integer not null,
    retry_count integer not null,
    started_at datetime(6),
    status enum ('ACTIVE','CANCELED','INACTIVE','PAUSED','PENDING','REFUNDED') not null,
    method_id bigint,
    user_id bigint not null,
    primary key (subscription_id),
    constraint fk_subscription_user foreign key (user_id) references `user` (user_id),
    constraint fk_subscription_method foreign key (method_id) references payment_method (method_id)
) engine=InnoDB;

create table payment_history (
    payment_id bigint not null auto_increment,
    amount integer not null,
    approved_at datetime(6),
    created_at datetime(6),
    fail_reason text,
    method_type enum ('BANK','CARD'),
    order_id varchar(100) not null,
    order_name varchar(255),
    payment_key varchar(255),
    status enum ('ABORTED','CANCELED','DONE','EXPIRED','FAILED','IN_PROGRESS','PARTIAL_CANCELED','READY') not null,
    method_id bigint,
    subscription_id bigint,
    user_id bigint not null,
    primary key (payment_id),
    constraint uk_payment_history_order_id unique (order_id),
    constraint fk_payment_history_user foreign key (user_id) references `user` (user_id),
    constraint fk_payment_history_subscription foreign key (subscription_id) references subscription (subscription_id),
    constraint fk_payment_history_method foreign key (method_id) references payment_method (method_id)
) engine=InnoDB;
