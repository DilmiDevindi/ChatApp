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

        // Set up UI components
        initializeUI();

        // Load initial data
        loadUsers();
        loadLogs();

        // Add window closing handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logout();
            }
        });
    }

    /**
     * Initialize the UI components.
     */
    private void initializeUI() {
        // Define colors for a consistent theme
        Color primaryColor = new Color(52, 73, 85);     // Dark blue-gray
        Color secondaryColor = new Color(249, 170, 51); // Orange
        Color backgroundColor = new Color(232, 237, 241); // Light gray-blue
        Color textColor = new Color(35, 47, 52);        // Dark gray
        Color buttonColor = new Color(74, 101, 114);    // Medium blue-gray

        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(backgroundColor);

        // Create header panel with gradient
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, primaryColor, 0, h, buttonColor);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        headerPanel.setPreferredSize(new Dimension(900, 60));
        headerPanel.setLayout(new BorderLayout());

        // Add title to header
        JLabel titleLabel = new JLabel("Admin Dashboard - " + adminUser.getUsername(), JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Top panel for logout (inside header)
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutPanel.setOpaque(false);
        logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Arial", Font.BOLD, 14));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setBackground(secondaryColor);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        logoutPanel.add(logoutButton);
        headerPanel.add(logoutPanel, BorderLayout.EAST);

        // Create tabbed pane with custom styling
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        tabbedPane.setBackground(backgroundColor);
        tabbedPane.setForeground(textColor);

        // User management tab
        JPanel userPanel = createUserPanel();
        tabbedPane.addTab("User Management", userPanel);

        // Chat management tab
        JPanel chatPanel = createChatPanel();
        tabbedPane.addTab("Chat Management", chatPanel);

        // Logs tab
        JPanel logPanel = createLogPanel();
        tabbedPane.addTab("System Logs", logPanel);

        // Add a footer
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(primaryColor);
        footerPanel.setPreferredSize(new Dimension(900, 30));

        // Add components to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        add(mainPanel);

        // Add action listeners
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });
    }

    /**
     * Create the user management panel.
     */
    private JPanel createUserPanel() {
        // Define colors for a consistent theme
        Color primaryColor = new Color(52, 73, 85);     // Dark blue-gray
        Color secondaryColor = new Color(249, 170, 51); // Orange
        Color backgroundColor = new Color(232, 237, 241); // Light gray-blue
        Color textColor = new Color(35, 47, 52);        // Dark gray
        Color buttonColor = new Color(74, 101, 114);    // Medium blue-gray

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table model with columns
        userTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        userTableModel.addColumn("ID");
        userTableModel.addColumn("Username");
        userTableModel.addColumn("Email");
        userTableModel.addColumn("Admin");
        userTableModel.addColumn("Online");

        // Create table with styling
        userTable = new JTable(userTableModel);
        userTable.setFont(new Font("Arial", Font.PLAIN, 14));
        userTable.setRowHeight(25);
        userTable.setGridColor(new Color(200, 200, 200));
        userTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        userTable.getTableHeader().setBackground(primaryColor);
        userTable.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(primaryColor));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Button panel with styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(backgroundColor);

        refreshUsersButton = new JButton("Refresh Users");
        refreshUsersButton.setFont(new Font("Arial", Font.BOLD, 14));
        refreshUsersButton.setForeground(Color.WHITE);
        refreshUsersButton.setBackground(buttonColor);
        refreshUsersButton.setFocusPainted(false);
        refreshUsersButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        removeUserButton = new JButton("Remove User");
        removeUserButton.setFont(new Font("Arial", Font.BOLD, 14));
        removeUserButton.setForeground(Color.WHITE);
        removeUserButton.setBackground(secondaryColor);
        removeUserButton.setFocusPainted(false);
        removeUserButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        buttonPanel.add(refreshUsersButton);
        buttonPanel.add(removeUserButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add action listeners
        refreshUsersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadUsers();
            }
        });

        removeUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedUser();
            }
        });

        return panel;
    }

    /**
     * Create the chat management panel.
     */
    private JPanel createChatPanel() {
        // Define colors for a consistent theme
        Color primaryColor = new Color(52, 73, 85);     // Dark blue-gray
        Color secondaryColor = new Color(249, 170, 51); // Orange
        Color backgroundColor = new Color(232, 237, 241); // Light gray-blue
        Color textColor = new Color(35, 47, 52);        // Dark gray
        Color buttonColor = new Color(74, 101, 114);    // Medium blue-gray

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table model with columns
        chatTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        chatTableModel.addColumn("ID");
        chatTableModel.addColumn("Name");
        chatTableModel.addColumn("Description");
        chatTableModel.addColumn("Creator");
        chatTableModel.addColumn("Members");

        // Create table with styling
        chatTable = new JTable(chatTableModel);
        chatTable.setFont(new Font("Arial", Font.PLAIN, 14));
        chatTable.setRowHeight(25);
        chatTable.setGridColor(new Color(200, 200, 200));
        chatTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        chatTable.getTableHeader().setBackground(primaryColor);
        chatTable.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(chatTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(primaryColor));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Button panel with styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(backgroundColor);

        JButton refreshChatsButton = new JButton("Refresh Chats");
        refreshChatsButton.setFont(new Font("Arial", Font.BOLD, 14));
        refreshChatsButton.setForeground(Color.WHITE);
        refreshChatsButton.setBackground(buttonColor);
        refreshChatsButton.setFocusPainted(false);
        refreshChatsButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        createChatButton = new JButton("Create Chat");
        createChatButton.setFont(new Font("Arial", Font.BOLD, 14));
        createChatButton.setForeground(Color.WHITE);
        createChatButton.setBackground(secondaryColor);
        createChatButton.setFocusPainted(false);
        createChatButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        addUserToChatButton = new JButton("Add User to Chat");
        addUserToChatButton.setFont(new Font("Arial", Font.BOLD, 14));
        addUserToChatButton.setForeground(Color.WHITE);
        addUserToChatButton.setBackground(buttonColor);
        addUserToChatButton.setFocusPainted(false);
        addUserToChatButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        removeUserFromChatButton = new JButton("Remove User from Chat");
        removeUserFromChatButton.setFont(new Font("Arial", Font.BOLD, 14));
        removeUserFromChatButton.setForeground(Color.WHITE);
        removeUserFromChatButton.setBackground(secondaryColor);
        removeUserFromChatButton.setFocusPainted(false);
        removeUserFromChatButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        buttonPanel.add(refreshChatsButton);
        buttonPanel.add(createChatButton);
        buttonPanel.add(addUserToChatButton);
        buttonPanel.add(removeUserFromChatButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add action listeners
        refreshChatsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadChats();
            }
        });

        createChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewChat();
            }
        });

        addUserToChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addUserToSelectedChat();
            }
        });

        removeUserFromChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeUserFromSelectedChat();
            }
        });

        // Load initial data
        loadChats();

        return panel;
    }

    /**
     * Create the log viewing panel.
     */
    private JPanel createLogPanel() {
        // Define colors for a consistent theme
        Color primaryColor = new Color(52, 73, 85);     // Dark blue-gray
        Color secondaryColor = new Color(249, 170, 51); // Orange
        Color backgroundColor = new Color(232, 237, 241); // Light gray-blue
        Color textColor = new Color(35, 47, 52);        // Dark gray
        Color buttonColor = new Color(74, 101, 114);    // Medium blue-gray

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table model with columns
        logTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        logTableModel.addColumn("ID");
        logTableModel.addColumn("Timestamp");
        logTableModel.addColumn("User");
        logTableModel.addColumn("Action");
        logTableModel.addColumn("Details");

        // Create table with styling
        logTable = new JTable(logTableModel);
        logTable.setFont(new Font("Arial", Font.PLAIN, 14));
        logTable.setRowHeight(25);
        logTable.setGridColor(new Color(200, 200, 200));
        logTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        logTable.getTableHeader().setBackground(primaryColor);
        logTable.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(primaryColor));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Button panel with styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(backgroundColor);

        refreshLogsButton = new JButton("Refresh Logs");
        refreshLogsButton.setFont(new Font("Arial", Font.BOLD, 14));
        refreshLogsButton.setForeground(Color.WHITE);
        refreshLogsButton.setBackground(buttonColor);
        refreshLogsButton.setFocusPainted(false);
        refreshLogsButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        clearLogsButton = new JButton("Clear Logs");
        clearLogsButton.setFont(new Font("Arial", Font.BOLD, 14));
        clearLogsButton.setForeground(Color.WHITE);
        clearLogsButton.setBackground(secondaryColor);
        clearLogsButton.setFocusPainted(false);
        clearLogsButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        buttonPanel.add(refreshLogsButton);
        buttonPanel.add(clearLogsButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add action listeners
        refreshLogsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadLogs();
            }
        });

        clearLogsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearLogs();
            }
        });

        return panel;
    }

    /**
     * Load the list of users.
     */
    private void loadUsers() {
        try {
            List<ChatUser> users = userService.getAllUsers();

            // Clear table
            userTableModel.setRowCount(0);

            // Add users to table
            for (ChatUser user : users) {
                Object[] row = {
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.isAdmin() ? "Yes" : "No",
                        user.isOnline() ? "Yes" : "No"
                };
                userTableModel.addRow(row);
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading users: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Load the system logs.
     */
    private void loadLogs() {
        try {
            List<ChatLogs> logs = logService.getAllLogs();

            // Clear table
            logTableModel.setRowCount(0);

            // Add logs to table
            for (ChatLogs log : logs) {
                Object[] row = {
                        log.getId(),
                        DATE_FORMAT.format(log.getTimestamp()),
                        log.getUser() != null ? log.getUser().getUsername() : "System",
                        log.getAction(),
                        log.getDetails()
                };
                logTableModel.addRow(row);
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading logs: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Clear all system logs.
     */
    private void clearLogs() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all logs? This action cannot be undone.",
                "Confirm Clear Logs",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = logService.clearLogs();
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "Logs cleared successfully",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadLogs();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to clear logs",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(this,
                        "Error clearing logs: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Load the list of chat groups.
     */
    private void loadChats() {
        try {
            List<ChatGrp> chats = chatService.getAllGroups();

            // Clear table
            chatTableModel.setRowCount(0);

            // Add chats to table
            for (ChatGrp chat : chats) {
                Object[] row = {
                        chat.getId(),
                        chat.getName(),
                        chat.getDescription(),
                        chat.getCreator().getUsername(),
                        chat.getMembers().size()
                };
                chatTableModel.addRow(row);
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading chats: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Create a new chat group.
     */
    private void createNewChat() {
        // Show dialog to get chat details
        JTextField nameField = new JTextField();
        JTextField descriptionField = new JTextField();

        Object[] message = {
                "Chat Name:", nameField,
                "Description:", descriptionField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Create New Chat", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String description = descriptionField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Chat name cannot be empty.",
                        "Invalid Input",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                ChatGrp chat = chatService.createGroup(name, description, adminUser.getUsername());
                if (chat != null) {
                    JOptionPane.showMessageDialog(this,
                            "Chat '" + name + "' created successfully.",
                            "Chat Created",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Notify all subscribed and online users that the chat has started
                    chatService.notifyChatStarted(name, chat.getCreatedDate());

                    loadChats(); // Refresh the chat list
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to create chat. The chat name may already be in use.",
                            "Operation Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(this,
                        "Error creating chat: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Add a user to the selected chat.
     */
    private void addUserToSelectedChat() {
        int selectedRow = chatTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a chat group.",
                    "No Chat Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String chatName = (String) chatTableModel.getValueAt(selectedRow, 1);

        // Show dialog to get username
        String username = JOptionPane.showInputDialog(this,
                "Enter the username to add to chat '" + chatName + "':",
                "Add User to Chat",
                JOptionPane.QUESTION_MESSAGE);

        if (username != null && !username.trim().isEmpty()) {
            username = username.trim();
            try {
                boolean success = chatService.addUserToGroup(chatName, username);
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "User '" + username + "' added to chat '" + chatName + "' successfully.",
                            "User Added",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadChats(); // Refresh the chat list
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to add user to chat. The user may not exist or is already a member.",
                            "Operation Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(this,
                        "Error adding user to chat: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove a user from the selected chat.
     */
    private void removeUserFromSelectedChat() {
        int selectedRow = chatTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a chat group.",
                    "No Chat Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String chatName = (String) chatTableModel.getValueAt(selectedRow, 1);

        // Show dialog to get username
        String username = JOptionPane.showInputDialog(this,
                "Enter the username to remove from chat '" + chatName + "':",
                "Remove User from Chat",
                JOptionPane.QUESTION_MESSAGE);

        if (username != null && !username.trim().isEmpty()) {
            username = username.trim();
            try {
                boolean success = chatService.removeUserFromGroup(chatName, username);
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "User '" + username + "' removed from chat '" + chatName + "' successfully.",
                            "User Removed",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadChats(); // Refresh the chat list
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to remove user from chat. The user may not exist or is not a member.",
                            "Operation Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(this,
                        "Error removing user from chat: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove the selected user from the application.
     */
    private void removeSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a user to remove.",
                    "No User Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = (String) userTableModel.getValueAt(selectedRow, 1);
        String isAdmin = (String) userTableModel.getValueAt(selectedRow, 3);

        // Cannot remove admin users
        if (isAdmin.equals("Yes")) {
            JOptionPane.showMessageDialog(this,
                    "Cannot remove admin users.",
                    "Operation Not Allowed",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm removal
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove user '" + username + "'? This action cannot be undone.",
                "Confirm User Removal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = userService.removeUser(adminUser.getUsername(), username);
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "User '" + username + "' has been removed successfully.",
                            "User Removed",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadUsers(); // Refresh the user list
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to remove user '" + username + "'.",
                            "Operation Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(this,
                        "Error removing user: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Log out and close the application.
     */
    private void logout() {
        try {
            // Logout user
            boolean logoutSuccess = userService.logout(adminUser.getUsername());
            System.out.println("Logout success for admin user: " + logoutSuccess);

            // Ensure all resources are released
            userService = null;
            logService = null;
            chatService = null;

            // Close window and return to login
            dispose();

            // Use a small delay to ensure resources are properly released
            Timer timer = new Timer(100, e -> {
                SwingUtilities.invokeLater(() -> {
                    Login loginForm = new Login();
                    loginForm.setVisible(true);
                });
            });
            timer.setRepeats(false);
            timer.start();
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Error during logout: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
