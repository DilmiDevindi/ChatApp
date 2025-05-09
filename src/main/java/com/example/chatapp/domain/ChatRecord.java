package com.example.chatapp.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "chat_records")
public class ChatRecord implements Serializable {

