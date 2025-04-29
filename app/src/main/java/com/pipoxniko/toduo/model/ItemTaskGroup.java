package com.pipoxniko.toduo.model;

import java.util.List;

public class ItemTaskGroup {
    private String groupTitle;
    private List<ItemTask> tasks;
    private boolean isExpanded;

    public ItemTaskGroup(String groupTitle, List<ItemTask> tasks) {
        this.groupTitle = groupTitle;
        this.tasks = tasks;
        this.isExpanded = false;
    }

    public ItemTaskGroup(String groupTitle, List<ItemTask> tasks, boolean isExpanded) {
        this.groupTitle = groupTitle;
        this.tasks = tasks;
        this.isExpanded = isExpanded;
    }

    public String getGroupTitle() { return groupTitle; }
    public void setGroupTitle(String groupTitle) { this.groupTitle = groupTitle; }
    public List<ItemTask> getTasks() { return tasks; }
    public void setTasks(List<ItemTask> tasks) { this.tasks = tasks; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}