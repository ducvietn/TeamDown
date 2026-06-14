package com.teamup.service;

import com.teamup.dto.GroupCreateRequest;
import com.teamup.dto.GroupResponse;
import com.teamup.dto.TaskResponse;
import com.teamup.dto.UserResponse;
import com.teamup.model.Group;
import com.teamup.model.User;
import com.teamup.repository.GroupRepository;
import com.teamup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Transactional
    public GroupResponse createGroup(GroupCreateRequest request) {
        User leader = userRepository.findById(request.getLeaderId())
                .orElseThrow(() -> new IllegalArgumentException("Leader not found: " + request.getLeaderId()));

        Group group = Group.builder()
                .groupName(request.getGroupName())
                .classId(request.getClassId())
                .leader(leader)
                .members(new ArrayList<>())
                .build();

        // Leader is also a member
        group.getMembers().add(leader);

        Group saved = groupRepository.save(group);
        log.info("Group created: id={}, name={}, leader={}", saved.getGroupId(), saved.getGroupName(), leader.getUserId());
        return toResponse(saved);
    }

    public List<GroupResponse> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse getGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
    }

    @Transactional
    public GroupResponse addMember(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (group.getMembers().stream().anyMatch(m -> m.getUserId().equals(userId))) {
            throw new IllegalArgumentException("User is already a member of this group.");
        }

        group.getMembers().add(user);
        Group saved = groupRepository.save(group);
        log.info("User {} added to group {}", userId, groupId);
        return toResponse(saved);
    }

    @Transactional
    public void removeMember(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));

        boolean removed = group.getMembers().removeIf(m -> m.getUserId().equals(userId));
        if (!removed) {
            throw new IllegalArgumentException("User is not a member of this group.");
        }

        groupRepository.save(group);
        log.info("User {} removed from group {}", userId, groupId);
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        groupRepository.deleteById(groupId);
        log.info("Group deleted: groupId={}", groupId);
    }

    GroupResponse toResponse(Group group) {
        return GroupResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .classId(group.getClassId())
                .createdAt(group.getCreatedAt() != null ? group.getCreatedAt().format(DTF) : null)
                .leader(toUserResponse(group.getLeader()))
                .members(group.getMembers() != null
                        ? group.getMembers().stream().map(this::toUserResponse).collect(Collectors.toList())
                        : Collections.emptyList())
                .tasks(group.getTasks() != null
                        ? group.getTasks().stream().map(taskService::toResponse).collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DTF) : null)
                .build();
    }
}
