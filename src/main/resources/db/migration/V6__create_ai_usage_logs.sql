create table ai_usage_logs (
    id bigserial primary key,
    operation varchar(40) not null,
    model varchar(100) not null,
    input_chars integer not null default 0,
    output_chars integer not null default 0,
    audio_bytes bigint,
    success boolean not null,
    error_message text,
    question_id bigint,
    practice_attempt_id bigint,
    created_at timestamp not null,

    constraint fk_ai_usage_logs_question
        foreign key (question_id) references questions(id) on delete set null,

    constraint fk_ai_usage_logs_practice_attempt
        foreign key (practice_attempt_id) references practice_attempts(id) on delete set null
);

create index idx_ai_usage_logs_created_at
    on ai_usage_logs (created_at desc);

create index idx_ai_usage_logs_operation_model
    on ai_usage_logs (operation, model);
