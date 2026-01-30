package com.taskmanager.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.taskmanager.model.TaskStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskRequest {

    @NotBlank(message = "Название задачи обязательно")
    @Size(min = 3, max = 255, message = "Название задачи должно быть от 3 до 255 символов")
    private String title;

    @Size(max = 2000, message = "Описание не может превышать 2000 символов")
    private String description;

    @NotNull(message = "Статус задачи обязателен")
    private TaskStatus status;

    @NotNull(message = "Срок выполнения обязателен")
    @Future(message = "Срок выполнения должен быть в будущем")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    @NotNull(message = "ID пользователя обязателен")
    private Long userId;
}