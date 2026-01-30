package com.taskmanager.dto.response;

import com.taskmanager.model.Role;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
}