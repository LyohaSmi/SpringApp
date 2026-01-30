package com.taskmanager.controller;

import com.taskmanager.dto.request.TaskRequest;
import com.taskmanager.dto.response.TaskResponse;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or #request.userId == authentication.principal.id")
    public ResponseEntity<?> createTask(@Valid @RequestBody TaskRequest request) {
        try {
            Task task = taskService.createTask(
                    request.getTitle(),
                    request.getDescription(),
                    request.getStatus(),
                    request.getDeadline(),
                    request.getUserId()
            );

            TaskResponse response = convertToTaskResponse(task);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid status. Allowed values: " + Arrays.toString(TaskStatus.values()));
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<?> getUserTasks(@PathVariable Long userId) {
        try {
            List<Task> tasks = taskService.getUserTasks(userId);
            List<TaskResponse> responses = tasks.stream()
                    .map(this::convertToTaskResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found with id: " + userId);
        }
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or @taskSecurityService.isTaskOwner(#taskId, authentication.principal.id)")
    public ResponseEntity<?> getTaskById(@PathVariable Long taskId) {
        try {
            Task task = taskService.getTaskById(taskId);
            TaskResponse response = convertToTaskResponse(task);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Task not found with id: " + taskId);
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        List<TaskResponse> responses = tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TaskResponse>> getTasksByStatus(@PathVariable TaskStatus status) {
        List<Task> tasks = taskService.getTasksByStatus(status);
        List<TaskResponse> responses = tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or @taskSecurityService.isTaskOwner(#taskId, authentication.principal.id)")
    public ResponseEntity<?> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest request) {
        try {
            Task task = taskService.updateTask(
                    taskId,
                    request.getTitle(),
                    request.getDescription(),
                    request.getStatus(),
                    request.getDeadline(),
                    request.getUserId()
            );
            TaskResponse response = convertToTaskResponse(task);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Task not found with id: " + taskId);
        }
    }

    @PatchMapping("/{taskId}/status")
    @PreAuthorize("hasRole('ADMIN') or @taskSecurityService.isTaskOwner(#taskId, authentication.principal.id)")
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestParam TaskStatus status) {
        try {
            Task task = taskService.updateTaskStatus(taskId, status);
            TaskResponse response = convertToTaskResponse(task);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Task not found with id: " + taskId);
        }
    }

    @PatchMapping("/{taskId}/details")
    @PreAuthorize("hasRole('ADMIN') or @taskSecurityService.isTaskOwner(#taskId, authentication.principal.id)")
    public ResponseEntity<?> updateTaskDetails(
            @PathVariable Long taskId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description) {
        try {
            Task task = taskService.updateTaskTitleAndDescription(taskId, title, description);
            TaskResponse response = convertToTaskResponse(task);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Task not found with id: " + taskId);
        }
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or @taskSecurityService.isTaskOwner(#taskId, authentication.principal.id)")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId) {
        try {
            taskService.deleteTask(taskId);
            return ResponseEntity.ok("Task deleted successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Task not found with id: " + taskId);
        }
    }

    private TaskResponse convertToTaskResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setStatusDisplayName(task.getStatus().getDisplayName());
        response.setDeadline(task.getDeadline());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setUserId(task.getUser().getId());
        response.setUsername(task.getUser().getUsername());
        return response;
    }

    @GetMapping("/user/{userId}/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<TaskResponse>> getUserTasksByStatus(
            @PathVariable Long userId,
            @PathVariable TaskStatus status) {

        List<Task> tasks = taskService.getTasksByUserIdAndStatus(userId, status);
        List<TaskResponse> responses = tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}