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
    private String createdAt;
    private boolean completed; // Thêm trường completed
    private boolean isChecked;

    public ItemTask() {
    }

    public ItemTask(String coupleId, String title, String description, String deadline, String categoryId, String assignment, String status) {
        this.coupleId = coupleId;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.categoryId = categoryId;
        this.assignment = assignment;
        this.status = status;
        this.completed = false; // Mặc định chưa hoàn thành
        this.isChecked = false;
    }

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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.isChecked = completed; // Đồng bộ isChecked với completed
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
        this.completed = checked; // Đồng bộ completed với isChecked
    }

    public String getContent() {
        return title;
    }
}