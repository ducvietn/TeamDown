package com.teamup.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class GroupCreateRequest {

    @NotBlank(message = "Group name is required")
    @Size(max = 100)
    private String groupName;

    @NotBlank(message = "Class ID is required")
    @Size(max = 50)
    private String classId;

    @NotNull(message = "Leader ID is required")
    private Long leaderId;
}
