package com.els.javatheorytrainer.enums;

/**
 * Status of a question.
 *
 * DRAFT    - question is being edited and is not shown in practice.
 * ACTIVE   - question is ready and can be shown in practice.
 * ARCHIVED - soft-deleted/hidden question. We keep it in database, but do not show it.
 */
public enum QuestionStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
