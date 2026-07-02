package com.els.javatheorytrainer.dto;

import jakarta.validation.constraints.NotBlank;

public record TextToSpeechRequest(
        @NotBlank String text
) {
}
