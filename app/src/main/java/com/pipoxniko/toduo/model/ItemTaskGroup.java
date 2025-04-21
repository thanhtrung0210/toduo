package com.pipoxniko.toduo.model;

import java.util.List;

public class ItemTaskGroup {

    private String title;
    private List<ItemTask> taskList;
    private boolean isExpanded;

    public ItemTaskGroup(String title, List<ItemTask> taskList) {
        this.title = title;
        this.taskList = taskList;
        this.isExpanded = true; // Mặc định mở
    }

    public String getTitle() {
        return title;
    }

    public List<ItemTask> getTaskList() {
        return taskList;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}
