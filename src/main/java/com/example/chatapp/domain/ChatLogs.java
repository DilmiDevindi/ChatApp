package com.example.chatapp.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "chat_logs")
public class ChatLogs implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private ChatUser user;

    @Column(name = "action", nullable = false)
    private String action;


    @Column(name = "timestamp", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "chat_id")
    private String chatId;

    @Column(name = "file_path", length = 500)
    private String filePath;

    // Default constructor required by Hibernate
    public ChatLogs() {
    }

    public ChatLogs(ChatUser user, String action, String ipAddress, String details) {
        this.user = user;
        this.action = action;
        this.timestamp = new Date();
        this.ipAddress = ipAddress;
        this.details = details;
    }

    public ChatLogs(ChatUser user, String action, String ipAddress, String details, String chatId, String filePath) {
        this.user = user;
        this.action = action;
        this.timestamp = new Date();
        this.ipAddress = ipAddress;
        this.details = details;
        this.chatId = chatId;
        this.filePath = filePath;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatUser getUser() {
        return user;
    }

    public void setUser(ChatUser user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "ChatLogs{" +
                "id=" + id +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                ", ipAddress='" + ipAddress + '\'' +
                ", details='" + details + '\'' +
                ", chatId='" + chatId + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}

