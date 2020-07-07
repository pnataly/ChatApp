package com.example.chat.model;

public class Group {

    private String title;
    private String description;
    private String image;
    private String createdBy;
    private String time;

    public Group() { }

    public Group(String title, String description, String image) {
        this.title = title;
        this.description = description;
        this.image = image;
    }

    public Group(String title, String description, String image, String createdBy, String time) {
        this.title = title;
        this.description = description;
        this.image = image;
        this.createdBy = createdBy;
        this.time = time;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}


