package com.els.javatheorytrainer.dto;

import com.els.javatheorytrainer.enums.AiUsageOperation;

public record AiUsageSummary(
        AiUsageOperation operation,
        String model,
        long calls,
        long successfulCalls,
        long failedCalls,
        long inputChars,
        long outputChars,
        long audioBytes
) {
}
