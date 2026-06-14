package com.teamup.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class LeaderActionRequest {

    @NotNull(message = "Task ID is required")
    private Long taskId;

    private Long leaderId; // ID of the user performing the action
}
