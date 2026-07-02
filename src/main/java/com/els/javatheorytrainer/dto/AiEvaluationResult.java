package com.els.javatheorytrainer.dto;

import com.els.javatheorytrainer.enums.PracticeGrade;

public record AiEvaluationResult(
        Integer scorePercent,
        PracticeGrade suggestedGrade,
        String feedback,
        String details,
        String missingPoints,
        String wrongParts,
        String goodParts,
        String followUpSuggestion
) {
}
