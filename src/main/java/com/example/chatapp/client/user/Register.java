package com.example.chatapp.client.user;

import com.example.chatapp.domain.ChatUser;
import com.example.chatapp.rmi.UserService;
import com.example.chatapp.server.implementation.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Register extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField emailField;
    private JTextField nickNameField;
    private JTextField profilePictureField;
    private JButton profilePictureButton;
    private JButton registerButton;
    private JButton backButton;
    private JLabel statusLabel;
    private String selectedProfilePicture = "default.png";

    private static final String USER_SERVICE_NAME = "UserService";
    private static final int RMI_PORT = 1099;
    private static final String RMI_HOST = "localhost";


}


