package com.teamup.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class TaskCreateRequest {

    @NotBlank(message = "Task name is required")
    @Size(max = 200)
    private String taskName;

    private String description;

    private String deadline; // ISO format: "2026-07-01T23:59:59"

    @Min(value = 0, message = "Progress must be at least 0")
    @Max(value = 100, message = "Progress must not exceed 100")
    private Integer progress = 0;

    private Long groupId;

    private Long assignedToId;
}
