package com.pipoxniko.toduo.model;

public class ItemTask {
    private String id;
    private String coupleId;
    private String title;
    private String description;
    private String deadline;
    private String categoryId;
    private String assignment;
    private String status;
    private boolean completed;
    private String createdAt;
    private boolean checked;

    public ItemTask() {}

    public ItemTask(String id, String coupleId, String title, String description, String deadline,
                    String categoryId, String assignment, String status, boolean completed, String createdAt, boolean checked) {
        this.id = id;
        this.coupleId = coupleId;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.categoryId = categoryId;
        this.assignment = assignment;
        this.status = status;
        this.completed = completed;
        this.createdAt = createdAt;
        this.checked = checked;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCoupleId() { return coupleId; }
    public void setCoupleId(String coupleId) { this.coupleId = coupleId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getAssignment() { return assignment; }
    public void setAssignment(String assignment) { this.assignment = assignment; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
}