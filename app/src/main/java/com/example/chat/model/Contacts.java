package com.example.chat.model;

public class Contacts {

    private String name;
    private String status;
    private String image;
    private String uid;
    private String state;
    private String time;
    private String date;
    private String role;
    private String phone;
    private String notificationKey;
    private User_state user_state;

    public Contacts() { }

    public Contacts(String uid, String role) {
        this.uid = uid;
        this.role = role;
    }

    public Contacts(String name, String status, String image, String uid, String phone, String notificationKey, User_state user_state) {
        this.name = name;
        this.status = status;
        this.image = image;
        this.uid = uid;
        this.phone = phone;
        this.notificationKey = notificationKey;
        this.user_state = user_state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public User_state getUser_state() {
        return user_state;
    }

    public void setUser_state(User_state user_state) {
        this.user_state = user_state;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNotificationKey() {
        return notificationKey;
    }

    public void setNotificationKey(String notificationKey) {
        this.notificationKey = notificationKey;
    }
}
