package com.els.javatheorytrainer.dto;

public record PracticeProgressStats(
        Long ownerId,
        Long totalQuestions,
        Long openedQuestions,
        Long answeredQuestions,
        Long masteredQuestions,
        Long totalAttempts,
        Long againCount,
        Long hardCount,
        Long goodCount,
        Long easyCount
) {

    public String progressText() {
        return value(masteredQuestions) + " / " + value(openedQuestions) + " / " + value(totalQuestions);
    }

    public String gradeText() {
        return "A:" + value(againCount)
                + " H:" + value(hardCount)
                + " G:" + value(goodCount)
                + " E:" + value(easyCount);
    }

    public static PracticeProgressStats empty(Long ownerId) {
        return new PracticeProgressStats(ownerId, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    private long value(Long number) {
        return number == null ? 0 : number;
    }
}
