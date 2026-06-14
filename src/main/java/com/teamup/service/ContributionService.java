package com.teamup.service;

import com.teamup.dto.ContributionResponse;
import com.teamup.model.Task;
import com.teamup.model.User;
import com.teamup.repository.GroupRepository;
import com.teamup.repository.TaskRepository;
import com.teamup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContributionService {

    private final TaskRepository taskRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    /**
     * Calculate contribution percentages for ALL members in a group.
     *
     * Logic:
     *   - For each member, collect all tasks assigned to them in the group.
     *   - Compute the average progress across all their assigned tasks.
     *   - Sum the average progress of all members → total.
     *   - Each member's contribution % = (member_avg / total) * 100.
     *
     * If a member has no tasks, their average = 0.
     */
    public List<ContributionResponse> calculateGroupContributions(Long groupId) {
        var groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }

        var group = groupOpt.get();

        // Collect all member IDs: leader + explicit members
        Set<Long> memberIds = new HashSet<>();
        if (group.getLeader() != null) {
            memberIds.add(group.getLeader().getUserId());
        }
        group.getMembers().forEach(m -> memberIds.add(m.getUserId()));

        List<User> members = userRepository.findByIdIn(new ArrayList<>(memberIds));

        // Fetch all tasks for this group in one query
        List<Task> allTasks = taskRepository.findByGroupGroupId(groupId);

        // Group tasks by assignee
        Map<Long, List<Task>> tasksByUser = allTasks.stream()
                .filter(t -> t.getAssignedTo() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignedTo().getUserId()));

        // Calculate average progress per member
        Map<Long, Double> avgProgressByUser = new HashMap<>();
        for (User member : members) {
            List<Task> userTasks = tasksByUser.getOrDefault(member.getUserId(), Collections.emptyList());
            if (userTasks.isEmpty()) {
                avgProgressByUser.put(member.getUserId(), 0.0);
            } else {
                double avg = userTasks.stream()
                        .mapToInt(Task::getProgress)
                        .average()
                        .orElse(0.0);
                avgProgressByUser.put(member.getUserId(), avg);
            }
        }

        // Sum of all averages (used as denominator)
        double totalAvg = avgProgressByUser.values().stream().mapToDouble(Double::doubleValue).sum();

        if (totalAvg == 0) {
            // All members have 0 contribution → distribute equally
            double equalShare = 100.0 / members.size();
            return members.stream()
                    .map(m -> buildContribution(m, equalShare, tasksByUser.getOrDefault(m.getUserId(), Collections.emptyList())))
                    .collect(Collectors.toList());
        }

        return members.stream()
                .map(m -> {
                    double avg = avgProgressByUser.getOrDefault(m.getUserId(), 0.0);
                    double contribution = (avg / totalAvg) * 100.0;
                    return buildContribution(m, contribution, tasksByUser.getOrDefault(m.getUserId(), Collections.emptyList()));
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate contribution for a specific user within a specific group.
     */
    public ContributionResponse calculateUserContribution(Long userId, Long groupId) {
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        var user = userOpt.get();

        List<Task> allTasks = taskRepository.findByGroupGroupId(groupId);
        List<Task> userTasks = allTasks.stream()
                .filter(t -> t.getAssignedTo() != null && t.getAssignedTo().getUserId().equals(userId))
                .collect(Collectors.toList());

        List<ContributionResponse> allContributions = calculateGroupContributions(groupId);
        return allContributions.stream()
                .filter(c -> c.getUserId().equals(userId))
                .findFirst()
                .orElse(buildContribution(user, 0.0, userTasks));
    }

    private ContributionResponse buildContribution(User user, double contribution, List<Task> tasks) {
        int taskCount = tasks.size();
        double avgProgress = taskCount > 0
                ? tasks.stream().mapToInt(Task::getProgress).average().orElse(0.0)
                : 0.0;

        return ContributionResponse.builder()
                .userId(user.getUserId())
                .userName(user.getName())
                .contributionPercent(Math.round(contribution * 100.0) / 100.0)
                .taskCount(taskCount)
                .averageProgress(Math.round(avgProgress * 100.0) / 100.0)
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }
}
