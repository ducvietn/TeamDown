package com.teamup.controller;

import com.teamup.dto.*;
import com.teamup.service.ContributionService;
import com.teamup.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupService groupService;
    private final ContributionService contributionService;

    // ── Create ─────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody GroupCreateRequest request) {
        GroupResponse group = groupService.createGroup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Group created", group));
    }

    // ── Read ──────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getAllGroups() {
        List<GroupResponse> groups = groupService.getAllGroups();
        return ResponseEntity.ok(ApiResponse.ok(groups));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroup(@PathVariable Long groupId) {
        GroupResponse group = groupService.getGroupById(groupId);
        return ResponseEntity.ok(ApiResponse.ok(group));
    }

    // ── Members ────────────────────────────────

    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<ApiResponse<GroupResponse>> addMember(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        GroupResponse group = groupService.addMember(groupId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member added", group));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        groupService.removeMember(groupId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member removed", null));
    }

    // ── Contributions ─────────────────────────

    @GetMapping("/{groupId}/contributions")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> getContributions(
            @PathVariable Long groupId) {
        List<ContributionResponse> contributions = contributionService.calculateGroupContributions(groupId);
        return ResponseEntity.ok(ApiResponse.ok(contributions));
    }

    // ── Delete ────────────────────────────────

    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long groupId) {
        groupService.deleteGroup(groupId);
        return ResponseEntity.ok(ApiResponse.ok("Group deleted", null));
    }
}
