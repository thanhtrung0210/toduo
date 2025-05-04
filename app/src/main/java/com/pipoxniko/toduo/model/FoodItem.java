package com.pipoxniko.toduo.model;

public class FoodItem {
    private String id;
    private String name;
    private String description;
    private String date;
    private String imageBase64;

    public FoodItem() {
    }

    public FoodItem(String id, String name, String description, String date, String imageBase64) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.date = date;
        this.imageBase64 = imageBase64;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}