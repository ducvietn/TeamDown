package com.teamup.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UserResponse {

    private Long userId;
    private String name;
    private String email;
    private String role;
    private String createdAt;
}
