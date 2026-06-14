package com.teamup.controller;

import com.teamup.dto.*;
import com.teamup.service.ContributionService;
import com.teamup.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService taskService;
    private final ContributionService contributionService;

    // ── Create ──────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskCreateRequest request) {
        TaskResponse task = taskService.createTask(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Task created", task));
    }

    // ── Read ───────────────────────────────────

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long taskId) {
        TaskResponse task = taskService.getTask(taskId);
        return ResponseEntity.ok(ApiResponse.ok(task));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByGroup(@PathVariable Long groupId) {
        List<TaskResponse> tasks = taskService.getTasksByGroup(groupId);
        return ResponseEntity.ok(ApiResponse.ok(tasks));
    }

    @GetMapping("/assignee/{userId}")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByAssignee(@PathVariable Long userId) {
        List<TaskResponse> tasks = taskService.getTasksByAssignee(userId);
        return ResponseEntity.ok(ApiResponse.ok(tasks));
    }

    // ── Update Progress ────────────────────────

    @PatchMapping("/{taskId}/progress")
    public ResponseEntity<ApiResponse<TaskResponse>> updateProgress(
            @PathVariable Long taskId,
            @RequestParam Integer progress) {
        TaskResponse task = taskService.updateProgress(taskId, progress);
        return ResponseEntity.ok(ApiResponse.ok("Progress updated", task));
    }

    // ── Submit for Review (100% + file) ───────

    /**
     * Submit with a URL string (legacy/backwards-compatible).
     */
    @PostMapping("/{taskId}/submit")
    public ResponseEntity<ApiResponse<TaskResponse>> submitTask(
            @PathVariable Long taskId,
            @RequestParam String fileUrl) {
        TaskResponse task = taskService.submitTask(taskId, fileUrl);
        return ResponseEntity.ok(ApiResponse.ok("Task submitted for review", task));
    }

    /**
     * Submit with a PDF file uploaded directly.
     * The file is stored in MongoDB GridFS, then the resulting gridFsFileId
     * is saved in the PostgreSQL Submission record.
     */
    @PostMapping(value = "/{taskId}/submit-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TaskResponse>> submitTaskWithFile(
            @PathVariable Long taskId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam String uploaderId) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is required"));
        }
        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only PDF files are accepted"));
        }

        TaskResponse task = taskService.submitTask(taskId, file, uploaderId);
        return ResponseEntity.ok(ApiResponse.ok("Task submitted for review", task));
    }

    // ── Leader: Approve / Reject ───────────────

    @PostMapping("/{taskId}/approve")
    public ResponseEntity<ApiResponse<TaskResponse>> approveTask(
            @PathVariable Long taskId,
            @RequestParam Long leaderId) {
        TaskResponse task = taskService.approveTask(taskId, leaderId);
        return ResponseEntity.ok(ApiResponse.ok("Task approved and completed", task));
    }

    @PostMapping("/{taskId}/reject")
    public ResponseEntity<ApiResponse<TaskResponse>> rejectTask(
            @PathVariable Long taskId,
            @RequestParam Long leaderId) {
        TaskResponse task = taskService.rejectTask(taskId, leaderId);
        return ResponseEntity.ok(ApiResponse.ok("Task rejected", task));
    }

    // ── Update Details ─────────────────────────

    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskUpdateRequest request) {
        TaskResponse task = taskService.updateTask(taskId, request);
        return ResponseEntity.ok(ApiResponse.ok("Task updated", task));
    }

    // ── Delete ─────────────────────────────────

    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok(ApiResponse.ok("Task deleted", null));
    }

    // ── Submissions ─────────────────────────────

    @GetMapping("/{taskId}/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissions(@PathVariable Long taskId) {
        List<SubmissionResponse> submissions = taskService.getSubmissionsByTask(taskId);
        return ResponseEntity.ok(ApiResponse.ok(submissions));
    }

    // ── Contribution ────────────────────────────

    @GetMapping("/contributions/group/{groupId}")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> getGroupContributions(
            @PathVariable Long groupId) {
        List<ContributionResponse> contributions = contributionService.calculateGroupContributions(groupId);
        return ResponseEntity.ok(ApiResponse.ok(contributions));
    }

    @GetMapping("/contributions/user/{userId}")
    public ResponseEntity<ApiResponse<ContributionResponse>> getUserContribution(
            @PathVariable Long userId,
            @RequestParam Long groupId) {
        ContributionResponse contribution = contributionService.calculateUserContribution(userId, groupId);
        return ResponseEntity.ok(ApiResponse.ok(contribution));
    }
}
