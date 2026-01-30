package com.taskmanager.dto.response;

import com.taskmanager.model.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private String statusDisplayName;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String username;
}