package com.markdownconverterpro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConvertRequest(
        @NotNull(message = "markdown is required")
        @Size(max = ConverterLimits.MAX_MARKDOWN_CHARACTERS,
              message = "markdown must not exceed 200000 characters")
        String markdown) {
}
