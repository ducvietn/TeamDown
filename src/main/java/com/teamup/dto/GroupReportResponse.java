package com.teamup.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class GroupReportResponse {

    private Long groupId;
    private String groupName;
    private String classId;
    private String generatedAt;
    private String generatedBy;

    /** Per-member report cards */
    private List<MemberReport> members;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberReport {
        private Long userId;
        private String name;
        private String email;
        private String role;
        private Double contributionPercent;
        private Integer taskCount;
        private Integer completedTaskCount;
        private Double averageProgress;

        /** Peer review attitude score (avg of all received scores 1–5) */
        private Double peerReviewScore;

        /** Number of peer reviews received */
        private Integer peerReviewCount;

        /** All submissions this member made */
        private List<SubmissionHistory> submissions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmissionHistory {
        private Long taskId;
        private String taskName;
        private String fileUrl;
        private String submittedAt;
        private String deadline;
        private boolean onTime;
    }
}
