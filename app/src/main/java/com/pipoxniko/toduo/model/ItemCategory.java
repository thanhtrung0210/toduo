package com.pipoxniko.toduo.model;

public class ItemCategory {
    private String id;
    private String coupleId;
    private String name;

    public ItemCategory() {
    }

    public ItemCategory(String id, String coupleId, String name) {
        this.id = id;
        this.coupleId = coupleId;
        this.name = name;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}