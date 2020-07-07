package com.example.chat.model;

public class User_state {

        private String state;
        private String time;
        private String date;
        private String typingTo;

        public User_state() { }

        public User_state(String state, String time, String date, String typingTo) {
            this.state = state;
            this.time = time;
            this.date = date;
            this.typingTo = typingTo;
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

        public String getTypingTo() {
            return typingTo;
        }
    
        public void setTypingTo(String typingTo) {
            this.typingTo = typingTo;
        }
}
