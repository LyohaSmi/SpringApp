package com.taskmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Data
@ToString(exclude = "user")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.TODO;  // МЕНЯЕМ ЗДЕСЬ!

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}