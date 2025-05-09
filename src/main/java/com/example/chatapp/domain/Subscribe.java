package com.example.chatapp.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "subscriptions")
public class Subscribe implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subscriber_id", nullable = false)
    private ChatUser subscriber;

    @ManyToOne
    @JoinColumn(name = "target_id", nullable = false)
    private ChatUser target;

    @Column(name = "subscription_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date subscriptionDate;

    @Column(name = "is_active")
    private boolean isActive = true;

    // Default constructor required by Hibernate
    public Subscribe() {
    }

    public Subscribe(ChatUser subscriber, ChatUser target) {
        this.subscriber = subscriber;
        this.target = target;
        this.subscriptionDate = new Date();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatUser getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(ChatUser subscriber) {
        this.subscriber = subscriber;
    }

    public ChatUser getTarget() {
        return target;
    }

    public void setTarget(ChatUser target) {
        this.target = target;
    }

    public Date getSubscriptionDate() {
        return subscriptionDate;
    }

    public void setSubscriptionDate(Date subscriptionDate) {
        this.subscriptionDate = subscriptionDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "Subscribe{" +
                "id=" + id +
                ", subscriber=" + (subscriber != null ? subscriber.getUsername() : "null") +
                ", target=" + (target != null ? target.getUsername() : "null") +
                ", subscriptionDate=" + subscriptionDate +
                ", isActive=" + isActive +
                '}';
    }
}
