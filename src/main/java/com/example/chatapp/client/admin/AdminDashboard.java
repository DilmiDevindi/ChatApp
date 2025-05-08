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

