package com.taskmanager.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        if (password.length() < 6) {
            return false;
        }
        if (!password.matches(".*\\d.*")) {
            return false;
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            return false;
        }
        return true;
    }
}