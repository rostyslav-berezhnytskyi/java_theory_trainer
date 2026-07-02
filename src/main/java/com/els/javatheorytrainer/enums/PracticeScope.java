package com.els.javatheorytrainer.enums;

public enum PracticeScope {
    SECTION("Section"),
    VOLUME("Whole volume");

    private final String label;

    PracticeScope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
