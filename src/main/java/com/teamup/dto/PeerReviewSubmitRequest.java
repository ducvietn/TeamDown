package com.teamup.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PeerReviewSubmitRequest {

    @NotNull(message = "Group ID is required")
    private Long groupId;

    @NotNull(message = "Reviewee ID is required")
    private Long revieweeId;

    @Min(value = 1, message = "Score must be at least 1")
    @Max(value = 5, message = "Score must not exceed 5")
    @NotNull(message = "Score is required")
    private Integer score;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}
