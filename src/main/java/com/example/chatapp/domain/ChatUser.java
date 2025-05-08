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

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    @Override
    public String toString() {
        return "ChatUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", nickName='" + nickName + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                ", isAdmin=" + isAdmin +
                ", isOnline=" + isOnline +
                '}';
    }
}
