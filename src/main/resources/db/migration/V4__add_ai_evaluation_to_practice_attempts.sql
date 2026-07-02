alter table practice_attempts
    add column ai_score_percent integer,
    add column ai_suggested_grade varchar(30),
    add column ai_feedback text,
    add column ai_details text,
    add column ai_missing_points text,
    add column ai_wrong_parts text,
    add column ai_good_parts text,
    add column ai_follow_up_suggestion text,
    add column ai_evaluated_at timestamp,
    add column ai_evaluation_error text;
