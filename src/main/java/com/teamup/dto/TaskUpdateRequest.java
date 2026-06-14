package com.teamup.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class TaskUpdateRequest {

    @Min(value = 0, message = "Progress must be at least 0")
    @Max(value = 100, message = "Progress must not exceed 100")
    private Integer progress;

    private String taskName;

    private String description;

    private String deadline;

    private Long assignedToId;
}
