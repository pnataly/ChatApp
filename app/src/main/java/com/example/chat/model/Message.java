package com.example.chat.model;

public class Message {

    private String from;
    private String type;
    private String message;
    private String to;
    private String messageID;
    private String time, date;
    private String name;

    public Message() { }

    public Message(String from, String type, String message, String to, String messageId, String time, String date, String name) {
        this.from = from;
        this.type = type;
        this.message = message;
        this.to = to;
        this.messageID = messageId;
        this.time = time;
        this.date = date;
        this.name = name;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMessageId() {
        return messageID;
    }

    public void setMessageId(String messageId) {
        this.messageID = messageId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
