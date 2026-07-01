create table practice_attempts (
                                   id bigserial primary key,
                                   question_id bigint not null,
                                   user_answer text not null,
                                   grade varchar(30),
                                   created_at timestamp not null,
                                   graded_at timestamp,

                                   constraint fk_practice_attempts_question
                                       foreign key (question_id) references questions(id) on delete cascade
);

create index idx_practice_attempts_question_created_at
    on practice_attempts (question_id, created_at desc);
