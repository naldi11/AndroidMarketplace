package com.octania.marketplace.data.model;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("id")
    private int id;

    @SerializedName("sender_id")
    private int senderId;

    @SerializedName("receiver_id")
    private int receiverId;

    @SerializedName("message")
    private String message;

    @SerializedName("is_read")
    private int isRead;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("attachment")
    private String attachment;

    @SerializedName("sender")
    private User sender;

    // Getters and Setters
    public int getId() { return id; }
    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public String getMessage() { return message; }
    public boolean isRead() { return isRead == 1; }
    public String getCreatedAt() { return createdAt; }
    public String getAttachment() { return attachment; }
    public User getSender() { return sender; }

    public void setId(int id) { this.id = id; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }
    public void setMessage(String message) { this.message = message; }
    public void setRead(boolean read) { isRead = read ? 1 : 0; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setAttachment(String attachment) { this.attachment = attachment; }
    public void setSender(User sender) { this.sender = sender; }
}
