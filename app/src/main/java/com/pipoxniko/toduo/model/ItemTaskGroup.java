package com.pipoxniko.toduo.model;

import java.util.List;

public class ItemTaskGroup {
    private String groupName;
    private List<ItemTask> taskList;
    private boolean isExpanded;

    public ItemTaskGroup(String groupName, List<ItemTask> taskList) {
        this.groupName = groupName;
        this.taskList = taskList;
        this.isExpanded = (taskList != null && !taskList.isEmpty()); // Mở mặc định nếu nhóm có task
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<ItemTask> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<ItemTask> taskList) {
        this.taskList = taskList;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}