package com.taskmanager.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/health")
    public String health() {
        return "Application is running!";
    }

    @GetMapping("/test")
    public String test() {
        return "Public endpoint - no authentication required";
    }
}