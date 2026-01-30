package com.taskmanager.service;

import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.model.User;
import com.taskmanager.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    @Transactional
    public Task createTask(String title, String description,
                           TaskStatus status, LocalDateTime deadline, Long userId) {

        User user = userService.findById(userId);

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setDeadline(deadline);
        task.setUser(user);

        return taskRepository.save(task);
    }

    public List<Task> getUserTasks(Long userId) {
        userService.findById(userId);
        return taskRepository.findByUserIdWithUser(userId);
    }

    public Task getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        task.getUser().getUsername();

        return task;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAllWithUser();  // Используем новый метод
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatusWithUser(status);  // Используем новый метод
    }

    public List<Task> getTasksByUserIdAndStatus(Long userId, TaskStatus status) {
        return taskRepository.findByUserIdAndStatusWithUser(userId, status);  // Используем новый метод
    }

    @Transactional
    public Task updateTask(Long taskId, String title, String description,
                           TaskStatus status, LocalDateTime deadline, Long userId) {

        Task task = getTaskById(taskId);
        User user = userService.findById(userId);

        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setDeadline(deadline);
        task.setUser(user);

        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, TaskStatus status) {
        Task task = getTaskById(taskId);
        task.setStatus(status);
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }
        taskRepository.deleteById(taskId);
    }

    @Transactional
    public Task updateTaskTitleAndDescription(Long taskId, String title, String description) {
        Task task = getTaskById(taskId);
        task.setTitle(title);
        task.setDescription(description);
        return taskRepository.save(task);
    }
}