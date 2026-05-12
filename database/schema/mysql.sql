create table if not exists crab_user (
    id bigint unsigned not null auto_increment primary key,
    username varchar(80) not null unique,
    display_name varchar(120) not null,
    password_hash varchar(255) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists saves (
    username varchar(255) not null primary key,
    crab_user_id bigint unsigned null,
    filepath varchar(255) not null,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    constraint fk_saves_crab_user foreign key (crab_user_id) references crab_user(id) on delete set null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
