create table volumes (
                         id bigserial primary key,
                         title varchar(150) not null unique,
                         slug varchar(150) not null unique,
                         description text,
                         sort_order integer not null default 0,
                         active boolean not null default true,
                         created_at timestamp not null,
                         updated_at timestamp not null
);

create table sections (
                          id bigserial primary key,
                          volume_id bigint not null,
                          title varchar(200) not null,
                          slug varchar(200) not null,
                          description text,
                          sort_order integer not null default 0,
                          active boolean not null default true,
                          created_at timestamp not null,
                          updated_at timestamp not null,

                          constraint fk_sections_volume
                              foreign key (volume_id) references volumes(id),

                          constraint uk_sections_volume_slug
                              unique (volume_id, slug)
);

create table questions (
                           id bigserial primary key,
                           section_id bigint not null,

                           question_text text not null,
                           short_answer text,
                           full_answer text,
                           hint text,

                           tags varchar(500),
                           source_reference varchar(300),

                           difficulty varchar(30) not null,
                           status varchar(30) not null,

                           sort_order integer not null default 0,

                           times_shown integer not null default 0,
                           total_attempts integer not null default 0,
                           correct_first_try_count integer not null default 0,
                           correct_total_count integer not null default 0,
                           wrong_total_count integer not null default 0,

                           last_shown_at timestamp,
                           last_answered_at timestamp,
                           next_review_at timestamp,

                           created_at timestamp not null,
                           updated_at timestamp not null,
                           version bigint,

                           constraint fk_questions_section
                               foreign key (section_id) references sections(id)
);

create table question_must_have_points (
                                           question_id bigint not null,
                                           position integer not null,
                                           point text not null,

                                           constraint pk_question_must_have_points
                                               primary key (question_id, position),

                                           constraint fk_question_must_have_points_question
                                               foreign key (question_id) references questions(id) on delete cascade
);

create table question_common_mistakes (
                                          question_id bigint not null,
                                          position integer not null,
                                          mistake text not null,

                                          constraint pk_question_common_mistakes
                                              primary key (question_id, position),

                                          constraint fk_question_common_mistakes_question
                                              foreign key (question_id) references questions(id) on delete cascade
);

create table question_images (
                                 id bigserial primary key,
                                 question_id bigint not null,
                                 role varchar(30) not null,
                                 image_url varchar(500) not null,
                                 alt_text varchar(300),
                                 caption text,
                                 sort_order integer not null default 0,

                                 constraint fk_question_images_question
                                     foreign key (question_id) references questions(id) on delete cascade
);