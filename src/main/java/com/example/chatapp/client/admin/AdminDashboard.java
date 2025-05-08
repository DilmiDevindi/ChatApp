package com.example.chatapp.client.admin;

import com.example.chatapp.client.user.Login;
import com.example.chatapp.domain.ChatLogs;
import com.example.chatapp.domain.ChatUser;
import com.example.chatapp.domain.ChatGrp;
import com.example.chatapp.rmi.ChatService;
import com.example.chatapp.rmi.LogService;
import com.example.chatapp.rmi.UserService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Admin dashboard for system administration.
 * Provides user management, log viewing, and system monitoring.
 */
public class AdminDashboard extends JFrame {
    private final ChatUser adminUser;
    private UserService userService;
    private LogService logService;

    private JTabbedPane tabbedPane;
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JTable logTable;
    private DefaultTableModel logTableModel;
    private JTable chatTable;
    private DefaultTableModel chatTableModel;
    private JButton refreshUsersButton;
    private JButton refreshLogsButton;
    private JButton clearLogsButton;
    private JButton logoutButton;
    private JButton removeUserButton;
    private JButton createChatButton;
    private JButton addUserToChatButton;
    private JButton removeUserFromChatButton;
    private ChatService chatService;

    private static final String USER_SERVICE_NAME = "UserService";
    private static final String LOG_SERVICE_NAME = "LogService";
    private static final String CHAT_SERVICE_NAME = "ChatService";
    private static final int RMI_PORT = 1099;
    private static final String RMI_HOST = "localhost";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor initializes the admin dashboard UI.
     *
     * @param user The admin user
     */
    public AdminDashboard(ChatUser user) {
        System.out.println("AdminDashboard constructor called with user: " + user.getUsername());
        this.adminUser = user;

        // Verify user is an admin
        System.out.println("Checking if user is admin. isAdmin=" + user.isAdmin());
        if (!user.isAdmin()) {
            System.out.println("Access denied: User is not an admin");
            JOptionPane.showMessageDialog(null,
                    "Access denied. Admin privileges required.",
                    "Access Denied",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        System.out.println("User is admin, continuing with dashboard initialization");

        setTitle("Chat Application - Admin Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize RMI services
        try {
            Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
            userService = (UserService) registry.lookup(USER_SERVICE_NAME);
            logService = (LogService) registry.lookup(LOG_SERVICE_NAME);
            chatService = (ChatService) registry.lookup(CHAT_SERVICE_NAME);
        } catch (RemoteException | NotBoundException e) {
            JOptionPane.showMessageDialog(this,
                    "Error connecting to server: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1);
        }
