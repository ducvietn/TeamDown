package com.teamup.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PeerReviewResponse {

    private Long reviewId;

    /** Score 1–5 */
    private Integer score;

    /** Reviewer's comment */
    private String comment;

    /** Group the review belongs to */
    private GroupSummary group;

    /** Reviewee info — always visible */
    private RevieweeInfo reviewee;

    private String createdAt;

    /** REVIEWER INFO IS NEVER EXPOSED to students */
    private RevieweeInfo reviewer;

    /** REVIEWEER INFO IS NEVER EXPOSED — this is intentional for anonymity */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevieweeInfo {
        private Long userId;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupSummary {
        private Long groupId;
        private String groupName;
    }
}
