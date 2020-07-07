package com.example.chat.model;

public class Comment {
    private String comment, comment_id;
    private String user_name, user_id, user_image;
    private String time;

    public Comment() { }

    public Comment(String comment, String comment_id, String user_name, String user_id, String user_image, String time) {
        this.comment = comment;
        this.comment_id = comment_id;
        this.user_name = user_name;
        this.user_id = user_id;
        this.user_image = user_image;
        this.time = time;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment_id() {
        return comment_id;
    }

    public void setComment_id(String comment_id) {
        this.comment_id = comment_id;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getUser_image() {
        return user_image;
    }

    public void setUser_image(String user_image) {
        this.user_image = user_image;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
