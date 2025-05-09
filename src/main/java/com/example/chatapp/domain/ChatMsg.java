package com.example.chatapp.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "chat_messages")
public class ChatMsg implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private ChatUser sender;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private ChatGrp group;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private ChatUser receiver;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "sent_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentTime;

    @Column(name = "is_read")
    private boolean isRead = false;

    // Default constructor required by Hibernate
    public ChatMsg() {
    }

    public ChatMsg(ChatUser sender, ChatUser receiver, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.sentTime = new Date();
    }

    public ChatMsg(ChatUser sender, ChatGrp group, String message) {
        this.sender = sender;
        this.group = group;
        this.message = message;
        this.sentTime = new Date();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatUser getSender() {
        return sender;
    }

    public void setSender(ChatUser sender) {
        this.sender = sender;
    }

    public ChatGrp getGroup() {
        return group;
    }

    public void setGroup(ChatGrp group) {
        this.group = group;
    }

    public ChatUser getReceiver() {
        return receiver;
    }

    public void setReceiver(ChatUser receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getSentTime() {
        return sentTime;
    }

    public void setSentTime(Date sentTime) {
        this.sentTime = sentTime;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    @Override
    public String toString() {
        return "ChatMsg{" +
                "id=" + id +
                ", sender=" + (sender != null ? sender.getUsername() : "null") +
                ", receiver=" + (receiver != null ? receiver.getUsername() : "null") +
                ", group=" + (group != null ? group.getName() : "null") +
                ", message='" + message + '\'' +
                ", sentTime=" + sentTime +
                ", isRead=" + isRead +
                '}';
    }
}
