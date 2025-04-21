package com.pipoxniko.toduo.model;

public class ItemTask {

    private String content;
    private boolean isChecked;

    public ItemTask(String content, boolean isChecked) {
        this.content = content;
        this.isChecked = isChecked;
    }

    public String getContent() {
        return content;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}
