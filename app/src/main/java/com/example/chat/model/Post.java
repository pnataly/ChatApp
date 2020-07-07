package com.example.chat.model;

public class Post {

    private String user_id, user_image, user_name;
    private String post_title, post_description, post_image;
    private String time;
    private String likes, comments;

    public Post() {
    }

    public Post(String user_id, String user_image, String user_name, String post_title, String post_description, String post_image, String time, String likes, String comments) {
        this.user_id = user_id;
        this.user_image = user_image;
        this.user_name = user_name;
        this.post_title = post_title;
        this.post_description = post_description;
        this.post_image = post_image;
        this.time = time;
        this.likes = likes;
        this.comments = comments;
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

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getPost_title() {
        return post_title;
    }

    public void setPost_title(String post_title) {
        this.post_title = post_title;
    }

    public String getPost_description() {
        return post_description;
    }

    public void setPost_description(String post_description) {
        this.post_description = post_description;
    }

    public String getPost_image() {
        return post_image;
    }

    public void setPost_image(String post_image) {
        this.post_image = post_image;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLikes() {
        return likes;
    }

    public void setLikes(String likes) {
        this.likes = likes;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
