package com.example.chatapp.client.user;

import com.example.chatapp.client.admin.AdminDashboard;
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

/**
 * Login interface for the chat application.
 * Allows users to log in or navigate to registration.
 */
public class Login extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;

    private static final String USER_SERVICE_NAME = "UserService";
    private static final int RMI_PORT = 1099;
    private static final String RMI_HOST = "localhost";

    /**
     * Constructor initializes the login UI.
     */
    public Login() {
        setTitle("Chat Application - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create components
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Chat Application Login", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        JLabel usernameLabel = new JLabel("Username:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(usernameLabel, gbc);

        usernameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(usernameField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(passwordField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        statusLabel = new JLabel("", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(statusLabel, gbc);

        add(panel);

        // Add action listeners
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openRegisterForm();
            }
        });
    }

    /**
     * Attempt to log in with the provided credentials.
     */
    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        System.out.println("Login attempt from client: username=" + username);

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password are required");
            System.out.println("Login failed: Username or password is empty");
            return;
        }

        // Check if server is running, start it if not
        System.out.println("Checking if server is running...");
        if (!ensureServerRunning()) {
            statusLabel.setText("Could not connect to server. Please try again later.");
            System.out.println("Login failed: Could not connect to server");
            return;
        }
        System.out.println("Server is running");

        try {
            System.out.println("Getting registry...");
            Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
            System.out.println("Looking up UserService...");
            UserService userService = (UserService) registry.lookup(USER_SERVICE_NAME);
            System.out.println("UserService found, calling login...");

            // Clear any previous login status
            System.out.println("Attempting login for: " + username);
            ChatUser user = userService.login(username, password);
            System.out.println("Login result: " + (user != null ? "Success" : "Failed"));

            if (user != null) {
                System.out.println("User details: username=" + user.getUsername() + ", isAdmin=" + user.isAdmin());
                dispose(); // Close login window

                if (user.isAdmin()) {
                    System.out.println("Opening AdminDashboard for admin user");
                    // Open admin dashboard
                    SwingUtilities.invokeLater(() -> {
                        try {
                            AdminDashboard dashboard = new AdminDashboard(user);
                            dashboard.setVisible(true);
                            System.out.println("Admin dashboard opened successfully");
                        } catch (Exception e) {
                            System.err.println("Error opening admin dashboard: " + e.getMessage());
                            e.printStackTrace();
                            // If admin dashboard fails to open, show login again
                            new Login().setVisible(true);
                        }
                    });
                } else {
                    System.out.println("Opening ChatLauncher for regular user");
                    // Open chat launcher
                    SwingUtilities.invokeLater(() -> {
                        new ChatLauncher(user).setVisible(true);
                    });
                }
            } else {
                statusLabel.setText("Invalid username or password");
                System.out.println("Login failed: Invalid username or password");
            }
        } catch (RemoteException | NotBoundException e) {
            statusLabel.setText("Error connecting to server: " + e.getMessage());
            System.out.println("Login failed: Error connecting to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ensures the server is running, starts it if necessary.
     *
     * @return true if server is running or was successfully started, false otherwise
     */
    private boolean ensureServerRunning() {
        try {
            // Try to connect to the server
            Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
            registry.lookup(USER_SERVICE_NAME);
            return true;
        } catch (RemoteException | NotBoundException e) {
            // Server not running, ask if user wants to start it
            int response = JOptionPane.showConfirmDialog(
                    this,
                    "Server is not running. Would you like to start it?",
                    "Start Server",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (response == JOptionPane.YES_OPTION) {
                return startServer();
            } else {
                return false;
            }
        }
    }

    /**
     * Start the server in a separate thread.
     *
     * @return true if server was successfully started, false otherwise
     */
    private boolean startServer() {
        try {
            statusLabel.setText("Starting server...");

            Thread serverThread = new Thread(() -> {
                try {
                    System.out.println("Starting server...");
                    Server server = new Server();
                    server.start();
                    System.out.println("Server started successfully.");
                } catch (Exception e) {
                    System.err.println("Error starting server: " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Failed to start server: " + e.getMessage());
                    });
                }
            });

            serverThread.setDaemon(true);
            serverThread.start();

            // Wait for server to start by checking if the registry is accessible
            System.out.println("Waiting for server to start...");
            boolean serverStarted = false;
            int maxRetries = 30; // Maximum number of retries (30 * 500ms = 15 seconds)
            int retries = 0;

            while (!serverStarted && retries < maxRetries) {
                try {
                    Thread.sleep(500); // Wait 500ms between checks
                    Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
                    registry.lookup(USER_SERVICE_NAME);
                    serverStarted = true;
                    System.out.println("Server is now running.");
                    statusLabel.setText("Server started successfully.");
                } catch (RemoteException | NotBoundException e) {
                    retries++;
                    System.out.println("Waiting for server... (" + retries + "/" + maxRetries + ")");
                    statusLabel.setText("Waiting for server... (" + retries + "/" + maxRetries + ")");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            return serverStarted;
        } catch (Exception e) {
            statusLabel.setText("Error starting server: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Open the registration form.
     */
    private void openRegisterForm() {
        dispose(); // Close login window
        SwingUtilities.invokeLater(() -> {
            new Register().setVisible(true);
        });
    }

    /**
     * Main method to start the login interface.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Login().setVisible(true);
        });
    }
}





