package edu.temple.mapchat.chat;

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
