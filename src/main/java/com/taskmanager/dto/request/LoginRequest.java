package com.taskmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Имя пользователя или email обязателен")
    private String usernameOrEmail;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}