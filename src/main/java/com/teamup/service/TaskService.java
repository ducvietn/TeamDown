package com.teamup.service;

import com.teamup.dto.*;
import com.teamup.model.*;
import com.teamup.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GridFSService gridFSService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ──────────────────────────────────────────────
    // Create
    // ──────────────────────────────────────────────

    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + request.getGroupId()));

        Task task = Task.builder()
                .taskName(request.getTaskName())
                .description(request.getDescription())
                .deadline(parseDeadline(request.getDeadline()))
                .progress(request.getProgress() != null ? request.getProgress() : 0)
                .status(TaskStatus.TODO)
                .group(group)
                .lastProgressUpdate(LocalDateTime.now())
                .build();

        if (request.getAssignedToId() != null) {
            User assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getAssignedToId()));
            task.setAssignedTo(assignee);
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        Task saved = taskRepository.save(task);
        log.info("Task created: id={}, name={}", saved.getTaskId(), saved.getTaskName());
        return toResponse(saved);
    }

    // ──────────────────────────────────────────────
    // Read
    // ──────────────────────────────────────────────

    public TaskResponse getTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        return toResponse(task);
    }

    public List<TaskResponse> getTasksByGroup(Long groupId) {
        return taskRepository.findByGroupIdWithDetails(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getTasksByAssignee(Long userId) {
        return taskRepository.findByAssignedToUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // Update Progress  (member action)
    // ──────────────────────────────────────────────

    @Transactional
    public TaskResponse updateProgress(Long taskId, Integer newProgress) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.DONE) {
            throw new IllegalStateException("Cannot update progress of a DONE task.");
        }

        if (newProgress < 0 || newProgress > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100.");
        }

        int previousProgress = task.getProgress();
        task.setProgress(newProgress);
        task.setLastProgressUpdate(LocalDateTime.now());

        if (newProgress > 0 && task.getStatus() == TaskStatus.TODO) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        if (newProgress < 100) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        Task saved = taskRepository.save(task);
        log.info("Progress updated: taskId={}, from={} to={}", taskId, previousProgress, newProgress);
        return toResponse(saved);
    }

    // ──────────────────────────────────────────────
    // Submit Task  (reaches 100% → require file → PENDING_REVIEW)
    // ──────────────────────────────────────────────

    /**
     * Submit Task  (reaches 100% → PDF uploaded to GridFS → PENDING_REVIEW)
     *
     * The student uploads the PDF directly to MongoDB GridFS first, then
     * calls this endpoint with the gridFsFileId returned by the upload API.
     */
    @Transactional
    public TaskResponse submitTask(Long taskId, String fileUrl) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.DONE) {
            throw new IllegalStateException("This task has already been approved by the leader.");
        }

        if (task.getProgress() < 100) {
            throw new IllegalStateException("Task progress must be 100% before submitting for review.");
        }

        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("File URL is required to submit a task.");
        }

        Submission submission = Submission.builder()
                .gridFsFileId(fileUrl)        // gridFsFileId — the ObjectId from MongoDB GridFS
                .originalFilename("submission.pdf") // fallback; overridden when student uploads via API
                .contentType("application/pdf")
                .fileSizeBytes(null)
                .task(task)
                .submittedAt(LocalDateTime.now())
                .build();
        submissionRepository.save(submission);

        task.setStatus(TaskStatus.PENDING_REVIEW);
        Task saved = taskRepository.save(task);

        log.info("Task submitted for review: taskId={}, gridFsFileId={}", taskId, fileUrl);
        return toResponse(saved);
    }

    /**
     * Submit Task with full GridFS metadata
     * Called by TaskController which receives the multipart upload.
     */
    @Transactional
    public TaskResponse submitTaskWithFile(Long taskId, String gridFsFileId,
                                           String originalFilename, Long fileSizeBytes) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.DONE) {
            throw new IllegalStateException("This task has already been approved by the leader.");
        }

        if (task.getProgress() < 100) {
            throw new IllegalStateException("Task progress must be 100% before submitting for review.");
        }

        Submission submission = Submission.builder()
                .gridFsFileId(gridFsFileId)
                .originalFilename(originalFilename)
                .contentType("application/pdf")
                .fileSizeBytes(fileSizeBytes)
                .task(task)
                .submittedAt(LocalDateTime.now())
                .build();
        submissionRepository.save(submission);

        task.setStatus(TaskStatus.PENDING_REVIEW);
        Task saved = taskRepository.save(task);

        log.info("Task submitted with GridFS file: taskId={}, gridFsFileId={}, filename={}",
                taskId, gridFsFileId, originalFilename);
        return toResponse(saved);
    }

    /**
     * Handles the full file upload → GridFS → submit workflow in one call.
     * 1. Validates the task
     * 2. Stores the PDF in MongoDB GridFS
     * 3. Saves the submission with the gridFsFileId
     */
    @Transactional
    public TaskResponse submitTask(Long taskId, org.springframework.web.multipart.MultipartFile file,
                                  String uploaderId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.DONE) {
            throw new IllegalStateException("This task has already been approved by the leader.");
        }

        if (task.getProgress() < 100) {
            throw new IllegalStateException("Task progress must be 100% before submitting for review.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }

        // 1. Store in MongoDB GridFS
        org.bson.types.ObjectId gridFsId = gridFSService.uploadFile(file, uploaderId, String.valueOf(taskId));

        // 2. Save submission record in PostgreSQL
        Submission submission = Submission.builder()
                .gridFsFileId(gridFsId.toHexString())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .task(task)
                .submittedAt(java.time.LocalDateTime.now())
                .build();
        submissionRepository.save(submission);

        // 3. Update task status
        task.setStatus(TaskStatus.PENDING_REVIEW);
        Task saved = taskRepository.save(task);

        log.info("Task submitted with GridFS upload: taskId={}, gridFsFileId={}, filename={}",
                taskId, gridFsId.toHexString(), file.getOriginalFilename());
        return toResponse(saved);
    }

    // ──────────────────────────────────────────────
    // Leader Actions: Approve / Reject
    // ──────────────────────────────────────────────

    @Transactional
    public TaskResponse approveTask(Long taskId, Long leaderId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        validateLeaderAuthority(task, leaderId);

        if (task.getStatus() != TaskStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only tasks in PENDING_REVIEW status can be approved.");
        }

        task.setStatus(TaskStatus.DONE);
        task.setProgress(100);

        Task saved = taskRepository.save(task);

        if (task.getAssignedTo() != null) {
            notificationService.sendTaskApproved(task, task.getAssignedTo());
        }

        log.info("Task approved: taskId={} by leader={}", taskId, leaderId);
        return toResponse(saved);
    }

    @Transactional
    public TaskResponse rejectTask(Long taskId, Long leaderId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        validateLeaderAuthority(task, leaderId);

        if (task.getStatus() != TaskStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only tasks in PENDING_REVIEW status can be rejected.");
        }

        task.setStatus(TaskStatus.IN_PROGRESS);

        Task saved = taskRepository.save(task);

        if (task.getAssignedTo() != null) {
            notificationService.sendTaskRejected(task, task.getAssignedTo());
        }

        log.info("Task rejected: taskId={} by leader={}, progress retained at {}",
                taskId, leaderId, task.getProgress());
        return toResponse(saved);
    }

    // ──────────────────────────────────────────────
    // Update full task details
    // ──────────────────────────────────────────────

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (request.getTaskName() != null) task.setTaskName(request.getTaskName());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getDeadline() != null) task.setDeadline(parseDeadline(request.getDeadline()));
        if (request.getAssignedToId() != null) {
            User assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getAssignedToId()));
            task.setAssignedTo(assignee);
        }

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    // ──────────────────────────────────────────────
    // Delete
    // ──────────────────────────────────────────────

    @Transactional
    public void deleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        taskRepository.deleteById(taskId);
        log.info("Task deleted: taskId={}", taskId);
    }

    // ──────────────────────────────────────────────
    // Submissions
    // ──────────────────────────────────────────────

    public List<SubmissionResponse> getSubmissionsByTask(Long taskId) {
        return submissionRepository.findByTaskTaskIdOrderBySubmittedAtDesc(taskId).stream()
                .map(this::toSubmissionResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────

    private void validateLeaderAuthority(Task task, Long leaderId) {
        Group group = task.getGroup();
        if (group == null || group.getLeader() == null) {
            throw new IllegalStateException("This task does not belong to any group.");
        }
        if (!group.getLeader().getUserId().equals(leaderId)) {
            throw new IllegalArgumentException("Only the group leader can perform this action.");
        }
    }

    private LocalDateTime parseDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) return null;
        try {
            return LocalDateTime.parse(deadline, DTF);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(deadline);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(DTF) : null;
    }

    TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .taskId(task.getTaskId())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .deadline(fmt(task.getDeadline()))
                .progress(task.getProgress())
                .status(task.getStatus().name())
                .lastProgressUpdate(fmt(task.getLastProgressUpdate()))
                .createdAt(fmt(task.getCreatedAt()))
                .updatedAt(fmt(task.getUpdatedAt()))
                .group(task.getGroup() != null ? TaskResponse.GroupInfo.builder()
                        .groupId(task.getGroup().getGroupId())
                        .groupName(task.getGroup().getGroupName())
                        .classId(task.getGroup().getClassId())
                        .build() : null)
                .assignedTo(task.getAssignedTo() != null ? TaskResponse.UserInfo.builder()
                        .userId(task.getAssignedTo().getUserId())
                        .name(task.getAssignedTo().getName())
                        .email(task.getAssignedTo().getEmail())
                        .build() : null)
                .build();
    }

    SubmissionResponse toSubmissionResponse(Submission s) {
        return SubmissionResponse.builder()
                .submissionId(s.getSubmissionId())
                .gridFsFileId(s.getGridFsFileId())
                .originalFilename(s.getOriginalFilename())
                .contentType(s.getContentType())
                .fileSizeBytes(s.getFileSizeBytes())
                .submittedAt(fmt(s.getSubmittedAt()))
                .taskId(s.getTask() != null ? s.getTask().getTaskId() : null)
                .taskName(s.getTask() != null ? s.getTask().getTaskName() : null)
                .build();
    }
}
