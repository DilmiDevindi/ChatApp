package com.example.chatapp.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "chat_records")
public class ChatRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private String chatId;

    @Column(name = "chat_name", nullable = false)
    private String chatName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "stop_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date stopTime;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    // Default constructor required by Hibernate
    public ChatRecord() {
        this.createdAt = new Date();
    }

    public ChatRecord(String chatId, String chatName, String filePath, Date stopTime) {
        this.chatId = chatId;
        this.chatName = chatName;
        this.filePath = filePath;
        this.stopTime = stopTime;
        this.createdAt = new Date();
    }
}