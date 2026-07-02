package com.els.javatheorytrainer.dto;

import com.els.javatheorytrainer.enums.PracticeQuestionFilter;
import com.els.javatheorytrainer.enums.PracticeScope;

public record PracticeSessionSettings(
        PracticeScope scope,
        Long volumeId,
        Long sectionId,
        PracticeQuestionFilter filter,
        boolean randomOrder
) {
    public static PracticeSessionSettings section(Long sectionId) {
        return new PracticeSessionSettings(
                PracticeScope.SECTION,
                null,
                sectionId,
                PracticeQuestionFilter.ALL_ACTIVE,
                false
        );
    }
}
