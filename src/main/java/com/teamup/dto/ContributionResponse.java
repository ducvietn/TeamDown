package com.teamup.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ContributionResponse {

    private Long userId;
    private String userName;
    private Double contributionPercent;
    private Integer taskCount;
    private Double averageProgress;
    private String role;
}
