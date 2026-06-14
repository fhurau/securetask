package com.securetask.backend.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Project fields accepted for create and update operations.")
public record ProjectRequest(
        @Schema(description = "Project name.", example = "Portfolio launch", maxLength = 100)
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(
                description = "Optional project description.",
                example = "Prepare the SecureTask demo",
                maxLength = 1000)
        @Size(max = 1000)
        String description) {
}
