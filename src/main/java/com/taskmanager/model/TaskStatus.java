package com.taskmanager.model;

public enum TaskStatus {
    TODO("To Do"),           // все задачи, которые нужно сделать
    IN_PROGRESS("In Progress"), // задачи, над которыми вы сейчас работаете
    DONE("Done");           // выполненные задачи

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}