package com.teamup.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class DashboardResponse {

    private Long groupId;
    private String groupName;
    private String classId;
    private String generatedAt;

    /** Contribution breakdown for every member in the group */
    private List<MemberContribution> members;

    /** Overall group health indicators */
    private GroupStats stats;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberContribution {
        private Long userId;
        private String name;
        private String email;
        private String role;
        private Double contributionPercent;
        private Integer taskCount;
        private Double averageProgress;
        private Integer completedTaskCount;
        private Integer pendingTaskCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupStats {
        private int totalTasks;
        private int completedTasks;
        private int pendingTasks;
        private int overdueTasks;
        private double overallCompletionPercent;
    }
}
