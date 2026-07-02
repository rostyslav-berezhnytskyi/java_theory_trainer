alter table questions
    add column again_count integer not null default 0,
    add column hard_count integer not null default 0,
    add column good_count integer not null default 0,
    add column easy_count integer not null default 0;

update questions q
set again_count = stats.again_count,
    hard_count = stats.hard_count,
    good_count = stats.good_count,
    easy_count = stats.easy_count
from (
    select question_id,
           count(*) filter (where grade = 'AGAIN')::integer as again_count,
           count(*) filter (where grade = 'HARD')::integer as hard_count,
           count(*) filter (where grade = 'GOOD')::integer as good_count,
           count(*) filter (where grade = 'EASY')::integer as easy_count
    from practice_attempts
    where grade is not null
    group by question_id
) stats
where q.id = stats.question_id;
