package com.els.javatheorytrainer.enums;

public enum PracticeQuestionFilter {
    ALL_ACTIVE("All active"),
    NEVER_OPENED("Never opened"),
    OPENED_NOT_ANSWERED("Opened but not answered"),
    NOT_MASTERED("Not mastered"),
    AGAIN_HARD("AGAIN/HARD only");

    private final String label;

    PracticeQuestionFilter(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
