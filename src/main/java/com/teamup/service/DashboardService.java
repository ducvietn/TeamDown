package com.teamup.service;

import com.teamup.dto.DashboardResponse;
import com.teamup.dto.GroupReportResponse;
import com.teamup.model.*;
import com.teamup.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final GroupRepository groupRepository;
    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;
    private final PeerReviewRepository peerReviewRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ──────────────────────────────────────────────
    // Real-time Dashboard — contribution % per member
    // ──────────────────────────────────────────────

    /**
     * Returns the live dashboard for a group:
     *   - Contribution % for each member (used for pie chart)
     *   - Task-level breakdown
     *   - Group-level statistics
     *
     * The contribution % is calculated as:
     *   member_avg_progress / sum(all_members_avg_progress) * 100
     */
    public DashboardResponse getGroupDashboard(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));

        List<Task> allTasks = taskRepository.findByGroupGroupId(groupId);
        List<Submission> allSubmissions = new ArrayList<>();
        for (Task task : allTasks) {
            allSubmissions.addAll(submissionRepository.findByTaskTaskIdOrderBySubmittedAtDesc(task.getTaskId()));
        }

        // Collect all member IDs
        Set<Long> memberIds = new HashSet<>();
        if (group.getLeader() != null) memberIds.add(group.getLeader().getUserId());
        group.getMembers().forEach(m -> memberIds.add(m.getUserId()));

        // Map tasks by assignee
        Map<Long, List<Task>> tasksByUser = allTasks.stream()
                .filter(t -> t.getAssignedTo() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignedTo().getUserId()));

        // Calculate avg progress per member
        Map<Long, Double> avgProgressMap = new HashMap<>();
        for (Long userId : memberIds) {
            List<Task> userTasks = tasksByUser.getOrDefault(userId, Collections.emptyList());
            double avg = userTasks.isEmpty() ? 0.0
                    : userTasks.stream().mapToInt(Task::getProgress).average().orElse(0.0);
            avgProgressMap.put(userId, avg);
        }

        double totalAvg = avgProgressMap.values().stream().mapToDouble(Double::doubleValue).sum();

        List<DashboardResponse.MemberContribution> memberContributions = new ArrayList<>();

        for (Long userId : memberIds) {
            User member = findMemberById(group, userId);
            if (member == null) continue;

            List<Task> userTasks = tasksByUser.getOrDefault(userId, Collections.emptyList());

            double avg = avgProgressMap.getOrDefault(userId, 0.0);
            double contribution = totalAvg > 0 ? (avg / totalAvg) * 100.0 : (100.0 / memberIds.size());

            int completed = (int) userTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE).count();
            int pending = (int) userTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS
                            || t.getStatus() == TaskStatus.PENDING_REVIEW).count();

            memberContributions.add(DashboardResponse.MemberContribution.builder()
                    .userId(userId)
                    .name(member.getName())
                    .email(member.getEmail())
                    .role(member.getRole() != null ? member.getRole().name() : null)
                    .contributionPercent(round(contribution))
                    .taskCount(userTasks.size())
                    .averageProgress(round(avg))
                    .completedTaskCount(completed)
                    .pendingTaskCount(pending)
                    .build());
        }

        // Group stats
        int totalTasks = allTasks.size();
        int completedTasks = (int) allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE).count();
        int overdueTasks = (int) allTasks.stream()
                .filter(t -> t.getDeadline() != null
                        && t.getDeadline().isBefore(LocalDateTime.now())
                        && t.getStatus() != TaskStatus.DONE).count();

        double overallCompletion = totalTasks > 0
                ? allTasks.stream().mapToInt(Task::getProgress).average().orElse(0.0)
                : 0.0;

        return DashboardResponse.builder()
                .groupId(groupId)
                .groupName(group.getGroupName())
                .classId(group.getClassId())
                .generatedAt(LocalDateTime.now().format(DTF))
                .members(memberContributions)
                .stats(DashboardResponse.GroupStats.builder()
                        .totalTasks(totalTasks)
                        .completedTasks(completedTasks)
                        .pendingTasks(totalTasks - completedTasks)
                        .overdueTasks(overdueTasks)
                        .overallCompletionPercent(round(overallCompletion))
                        .build())
                .build();
    }

    // ──────────────────────────────────────────────
    // Full report data — used for PDF/Excel export
    // ──────────────────────────────────────────────

    public GroupReportResponse buildGroupReport(Long groupId, Long teacherId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));

        List<Task> allTasks = taskRepository.findByGroupGroupId(groupId);

        Set<Long> memberIds = new HashSet<>();
        if (group.getLeader() != null) memberIds.add(group.getLeader().getUserId());
        group.getMembers().forEach(m -> memberIds.add(m.getUserId()));

        Map<Long, List<Task>> tasksByUser = allTasks.stream()
                .filter(t -> t.getAssignedTo() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignedTo().getUserId()));

        Map<Long, Double> avgProgressMap = new HashMap<>();
        for (Long userId : memberIds) {
            List<Task> tasks = tasksByUser.getOrDefault(userId, Collections.emptyList());
            avgProgressMap.put(userId, tasks.isEmpty() ? 0.0
                    : tasks.stream().mapToInt(Task::getProgress).average().orElse(0.0));
        }

        double totalAvg = avgProgressMap.values().stream().mapToDouble(Double::doubleValue).sum();

        List<GroupReportResponse.MemberReport> memberReports = new ArrayList<>();

        for (Long userId : memberIds) {
            User member = findMemberById(group, userId);
            if (member == null) continue;

            List<Task> userTasks = tasksByUser.getOrDefault(userId, Collections.emptyList());

            double avg = avgProgressMap.getOrDefault(userId, 0.0);
            double contribution = totalAvg > 0 ? (avg / totalAvg) * 100.0 : 0.0;

            int completedCount = (int) userTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE).count();

            Double peerAvg = peerReviewRepository.findAverageScoreByRevieweeAndGroup(userId, groupId);
            List<PeerReview> receivedReviews = peerReviewRepository.findByRevieweeUserId(userId);

            // Collect submissions for this member
            List<GroupReportResponse.SubmissionHistory> submissions = new ArrayList<>();
            for (Task task : userTasks) {
                List<Submission> taskSubs = submissionRepository
                        .findByTaskTaskIdOrderBySubmittedAtDesc(task.getTaskId());
                for (Submission sub : taskSubs) {
                    boolean onTime = task.getDeadline() == null
                            || sub.getSubmittedAt().isBefore(task.getDeadline());
                    submissions.add(GroupReportResponse.SubmissionHistory.builder()
                            .taskId(task.getTaskId())
                            .taskName(task.getTaskName())
                            .submittedAt(sub.getSubmittedAt().format(DTF))
                            .deadline(task.getDeadline() != null ? task.getDeadline().format(DTF) : null)
                            .onTime(onTime)
                            .build());
                }
            }

            memberReports.add(GroupReportResponse.MemberReport.builder()
                    .userId(userId)
                    .name(member.getName())
                    .email(member.getEmail())
                    .role(member.getRole() != null ? member.getRole().name() : null)
                    .contributionPercent(round(contribution))
                    .taskCount(userTasks.size())
                    .completedTaskCount(completedCount)
                    .averageProgress(round(avg))
                    .peerReviewScore(peerAvg != null ? round(peerAvg) : null)
                    .peerReviewCount(receivedReviews.size())
                    .submissions(submissions)
                    .build());
        }

        String teacherName = null;
        if (teacherId != null) {
            teacherName = "ID: " + teacherId; // Resolved name if User entity available
        }

        return GroupReportResponse.builder()
                .groupId(groupId)
                .groupName(group.getGroupName())
                .classId(group.getClassId())
                .generatedAt(LocalDateTime.now().format(DTF))
                .generatedBy(teacherName)
                .members(memberReports)
                .build();
    }

    // ──────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────

    private User findMemberById(Group group, Long userId) {
        if (group.getLeader() != null && group.getLeader().getUserId().equals(userId)) {
            return group.getLeader();
        }
        return group.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
