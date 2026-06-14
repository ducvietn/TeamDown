package com.teamup.controller;

import com.teamup.dto.*;
import com.teamup.service.PeerReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/peer-reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PeerReviewController {

    private final PeerReviewService peerReviewService;

    // ── Submit a peer review ──────────────────────────────────
    // The reviewer's ID comes from the authenticated session.
    // REVIEWER_ID is stored in DB for audit but NEVER returned to clients.

    @PostMapping
    public ResponseEntity<ApiResponse<PeerReviewResponse>> submitReview(
            @RequestParam Long reviewerId,
            @Valid @RequestBody PeerReviewSubmitRequest request) {
        PeerReviewResponse review = peerReviewService.submitReview(reviewerId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Peer review submitted", review));
    }

    // ── View reviews I received (anonymized — no reviewer info) ─
    // Students see only: score, comment, group — NO reviewer identity.

    @GetMapping("/received/{userId}")
    public ResponseEntity<ApiResponse<List<PeerReviewResponse>>> getReviewsReceived(
            @PathVariable Long userId) {
        List<PeerReviewResponse> reviews = peerReviewService.getReviewsReceived(userId);
        return ResponseEntity.ok(ApiResponse.ok(reviews));
    }

    // ── View reviews given (for self-audit) ────────────────────
    // The reviewer can see their own submitted reviews (with reviewee info).

    @GetMapping("/given/{reviewerId}")
    public ResponseEntity<ApiResponse<List<PeerReviewResponse>>> getReviewsGiven(
            @PathVariable Long reviewerId) {
        List<PeerReviewResponse> reviews = peerReviewService.getReviewsGiven(reviewerId);
        return ResponseEntity.ok(ApiResponse.ok(reviews));
    }

    // ── Group reviews — TEACHER ONLY ─────────────────────────
    // includeReviewer=true → shows who reviewed whom (for teacher transparency)
    // includeReviewer=false → same anonymized view as students

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<PeerReviewResponse>>> getGroupReviews(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "false") boolean includeReviewer) {
        List<PeerReviewResponse> reviews = peerReviewService.getGroupReviews(groupId, includeReviewer);
        return ResponseEntity.ok(ApiResponse.ok(reviews));
    }

    // ── Average peer score for a user in a group ─────────────

    @GetMapping("/average/{userId}")
    public ResponseEntity<ApiResponse<Double>> getAverageScore(
            @PathVariable Long userId,
            @RequestParam Long groupId) {
        Double avg = peerReviewService.getAverageScore(userId, groupId);
        return ResponseEntity.ok(ApiResponse.ok(avg != null ? avg : 0.0));
    }
}
