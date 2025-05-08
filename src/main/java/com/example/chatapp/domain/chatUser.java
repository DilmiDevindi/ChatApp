package com.example.chatapp.domain;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "chat_users")
public class ChatUser implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "nick_name")
    private String nickName;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "is_admin")
    private boolean isAdmin = false;

    @Column(name = "is_online")
    private boolean isOnline = false;

    // Default constructor required by Hibernate
    public ChatUser() {
    }

    public ChatUser(String username, String password, String email, String nickName, String profilePicture, boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.nickName = nickName;
        this.profilePicture = profilePicture;
        this.isAdmin = isAdmin;
    }

    public ChatUser(String username, String password, String email, boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.isAdmin = isAdmin;
    }
}
