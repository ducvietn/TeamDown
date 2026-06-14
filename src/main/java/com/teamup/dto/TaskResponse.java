package com.teamup.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class TaskResponse {

    private Long taskId;
    private String taskName;
    private String description;
    private String deadline;
    private Integer progress;
    private String status;
    private String lastProgressUpdate;
    private String createdAt;
    private String updatedAt;

    private GroupInfo group;
    private UserInfo assignedTo;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupInfo {
        private Long groupId;
        private String groupName;
        private String classId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long userId;
        private String name;
        private String email;
    }
}
