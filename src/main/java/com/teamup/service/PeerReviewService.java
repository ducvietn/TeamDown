package com.teamup.service;

import com.teamup.dto.PeerReviewResponse;
import com.teamup.dto.PeerReviewSubmitRequest;
import com.teamup.model.Group;
import com.teamup.model.PeerReview;
import com.teamup.model.User;
import com.teamup.repository.GroupRepository;
import com.teamup.repository.PeerReviewRepository;
import com.teamup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeerReviewService {

    private final PeerReviewRepository peerReviewRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ──────────────────────────────────────────────
    // Submit a peer review
    // ──────────────────────────────────────────────

    @Transactional
    public PeerReviewResponse submitReview(Long reviewerId, PeerReviewSubmitRequest request) {
        // The reviewer
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerId));

        // The group
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + request.getGroupId()));

        // Verify reviewer is a member of this group
        boolean isMember = group.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(reviewerId));
        if (!isMember && !group.getLeader().getUserId().equals(reviewerId)) {
            throw new IllegalArgumentException("You are not a member of this group.");
        }

        // The reviewee
        User reviewee = userRepository.findById(request.getRevieweeId())
                .orElseThrow(() -> new IllegalArgumentException("Reviewee not found: " + request.getRevieweeId()));

        // Prevent self-review
        if (reviewerId.equals(request.getRevieweeId())) {
            throw new IllegalArgumentException("You cannot review yourself.");
        }

        // Check if already reviewed
        if (peerReviewRepository.existsByGroupGroupIdAndReviewerUserIdAndRevieweeUserId(
                request.getGroupId(), reviewerId, request.getRevieweeId())) {
            throw new IllegalStateException("You have already reviewed this teammate for this group.");
        }

        PeerReview review = PeerReview.builder()
                .score(request.getScore())
                .comment(request.getComment())
                .group(group)
                .reviewer(reviewer)   // stored in DB for audit — NEVER exposed in responses
                .reviewee(reviewee)
                .build();

        PeerReview saved = peerReviewRepository.save(review);
        log.info("Peer review submitted: reviewer={}, reviewee={}, group={}, score={}",
                reviewerId, request.getRevieweeId(), request.getGroupId(), request.getScore());

        return toResponse(saved);
    }

    // ──────────────────────────────────────────────
    // Read reviews
    //
    // SECURITY: reviewer info is NEVER exposed to students.
    // Only reviewee info + score + comment are returned.
    // ──────────────────────────────────────────────

    /**
     * Returns all reviews received by a user (anonymized — no reviewer info).
     * Used for students viewing their own received feedback.
     */
    public List<PeerReviewResponse> getReviewsReceived(Long userId) {
        return peerReviewRepository.findByRevieweeUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all reviews for a group — teacher-only endpoint.
     * Includes reviewer info for transparency.
     */
    public List<PeerReviewResponse> getGroupReviews(Long groupId, boolean includeReviewer) {
        List<PeerReview> reviews = peerReviewRepository.findByGroupGroupId(groupId);

        if (includeReviewer) {
            return reviews.stream()
                    .map(this::toResponseWithReviewer)
                    .collect(Collectors.toList());
        } else {
            return reviews.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Returns the average score received by a user in a given group.
     */
    public Double getAverageScore(Long userId, Long groupId) {
        return peerReviewRepository.findAverageScoreByRevieweeAndGroup(userId, groupId);
    }

    /**
     * Returns all reviews given by a user (for the giver's own audit).
     * Again — NO reviewee identity is hidden from others (scores only).
     */
    public List<PeerReviewResponse> getReviewsGiven(Long reviewerId) {
        return peerReviewRepository.findByReviewerUserId(reviewerId).stream()
                .map(this::toAnonymizedResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // Private mapping helpers
    // ──────────────────────────────────────────────

    /**
     * Standard response — reviewee visible, reviewer HIDDEN (student view).
     */
    private PeerReviewResponse toResponse(PeerReview review) {
        return PeerReviewResponse.builder()
                .reviewId(review.getReviewId())
                .score(review.getScore())
                .comment(review.getComment())
                .group(review.getGroup() != null ? PeerReviewResponse.GroupSummary.builder()
                        .groupId(review.getGroup().getGroupId())
                        .groupName(review.getGroup().getGroupName())
                        .build() : null)
                .reviewee(review.getReviewee() != null ? PeerReviewResponse.RevieweeInfo.builder()
                        .userId(review.getReviewee().getUserId())
                        .name(review.getReviewee().getName())
                        .build() : null)
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().format(DTF) : null)
                .build();
    }

    /**
     * Teacher/admin view — reviewee + group + reviewer all visible.
     */
    private PeerReviewResponse toResponseWithReviewer(PeerReview review) {
        PeerReviewResponse resp = toResponse(review);
        // Add reviewer info — only for teachers
        resp.setReviewer(review.getReviewer() != null
                ? PeerReviewResponse.RevieweeInfo.builder()
                    .userId(review.getReviewer().getUserId())
                    .name(review.getReviewer().getName())
                    .build()
                : null);
        return resp;
    }

    /**
     * Self-audit view for the reviewer — shows what they submitted.
     * Reviewee identity is shown only to the reviewer themselves.
     */
    private PeerReviewResponse toAnonymizedResponse(PeerReview review) {
        return PeerReviewResponse.builder()
                .reviewId(review.getReviewId())
                .score(review.getScore())
                .comment(review.getComment())
                .group(review.getGroup() != null ? PeerReviewResponse.GroupSummary.builder()
                        .groupId(review.getGroup().getGroupId())
                        .groupName(review.getGroup().getGroupName())
                        .build() : null)
                .reviewee(review.getReviewee() != null ? PeerReviewResponse.RevieweeInfo.builder()
                        .userId(review.getReviewee().getUserId())
                        .name(review.getReviewee().getName())
                        .build() : null)
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().format(DTF) : null)
                .build();
    }
}
