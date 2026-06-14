package com.teamup.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class GroupResponse {

    private Long groupId;
    private String groupName;
    private String classId;
    private String createdAt;

    private UserResponse leader;

    private List<UserResponse> members;

    private List<TaskResponse> tasks;
}
