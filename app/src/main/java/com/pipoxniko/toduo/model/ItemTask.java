package com.pipoxniko.toduo.model;

public class ItemTask {
    private String id;
    private String coupleId;
    private String title;
    private String description;
    private String deadline; // Định dạng: "yyyy-MM-dd HH:mm"
    private String categoryId;
    private String assignment;
    private String status;
    private String createdAt;

    public ItemTask() {
        // Constructor mặc định cho Firebase
    }

    public ItemTask(String id, String coupleId, String title, String description, String deadline,
                    String categoryId, String assignment, String status, String createdAt) {
        this.id = id;
        this.coupleId = coupleId;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.categoryId = categoryId;
        this.assignment = assignment;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCoupleId() {
        return coupleId;
    }

    public void setCoupleId(String coupleId) {
        this.coupleId = coupleId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getAssignment() {
        return assignment;
    }

    public void setAssignment(String assignment) {
        this.assignment = assignment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // Để tương thích với logic hiện tại của ItemTaskAdapter
    public String getContent() {
        return title;
    }

    public boolean isChecked() {
        return "completed".equals(status);
    }

    public void setChecked(boolean checked) {
        this.status = checked ? "completed" : "normal";
    }
}