package com.taskmanager.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/health")
    public String health() {
        return "Application is running!";
    }

    @GetMapping("/test-db")
    public String testDb() {
        return "Database connection test - OK!";
    }
}