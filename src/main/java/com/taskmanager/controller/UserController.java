package com.taskmanager.controller;

import com.taskmanager.dto.request.RegisterRequest;
import com.taskmanager.dto.response.UserResponse;
import com.taskmanager.model.User;
import com.taskmanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.principal.username")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            User user = userService.findByUsername(username);
            UserResponse response = convertToUserResponse(user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found: " + username);
        }
    }

    @GetMapping("/id/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            UserResponse response = convertToUserResponse(user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found with id: " + id);
        }
    }

    // ============== АДМИНСКИЕ ENDPOINTS ==============

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.findAll();
        List<UserResponse> responses = users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role) {
        try {
            User user = userService.updateUserRole(id, role);
            UserResponse response = convertToUserResponse(user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        }
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        return response;
    }
}