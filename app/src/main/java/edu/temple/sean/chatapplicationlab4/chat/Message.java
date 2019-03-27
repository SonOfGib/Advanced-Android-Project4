package edu.temple.sean.chatapplicationlab4.chat;

import edu.temple.sean.chatapplicationlab4.Partner;

public class Message {

    private String sender;
    private String content;
    private long dateTime;

    public Message(String sender, String content){
        this.sender = sender;
        this.content = content;
        dateTime = System.currentTimeMillis();
    }
    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAt() {
        return dateTime;
    }
}
