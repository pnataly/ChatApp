package com.example.chat.model;

public class Notification {

    private String postId, time, notification, postUserId;
    private String senderId, senderName, senderImage;

    public Notification() { }

    public Notification(String postId, String time, String notification, String postUserId, String senderId, String senderName, String senderImage) {
        this.postId = postId;
        this.time = time;
        this.notification = notification;
        this.postUserId = postUserId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderImage = senderImage;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public String getPostUserId() {
        return postUserId;
    }

    public void setPostUserId(String postUserId) {
        this.postUserId = postUserId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderImage() {
        return senderImage;
    }

    public void setSenderImage(String senderImage) {
        this.senderImage = senderImage;
    }
}
