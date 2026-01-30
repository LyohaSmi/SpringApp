package com.taskmanager.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Пароль должен содержать минимум 6 символов, включая хотя бы одну цифру и одну букву";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}