package com.els.javatheorytrainer.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank String message
) {
}
