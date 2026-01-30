package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSecurityService {

    private final TaskRepository taskRepository;

    public boolean isTaskOwner(Long taskId, Long userId) {
        try {
            return taskRepository.findById(taskId)
                    .map(task -> {
                        boolean isOwner = task.getUser().getId().equals(userId);
                        log.debug("Task {} owned by user {}? {}", taskId, userId, isOwner);
                        return isOwner;
                    })
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error checking task ownership: {}", e.getMessage());
            return false;
        }
    }
}