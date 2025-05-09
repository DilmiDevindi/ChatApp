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

    /**
     * Constructor initializes the registration UI.
     */
    public Register() {
        setTitle("Chat Application - Register");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create components
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Create New Account", JLabel.CENTER);
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

        JLabel confirmPasswordLabel = new JLabel("Confirm Password:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(confirmPasswordLabel, gbc);

        confirmPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(confirmPasswordField, gbc);

        JLabel emailLabel = new JLabel("Email:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(emailLabel, gbc);

        emailField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 4;
        panel.add(emailField, gbc);

        JLabel nickNameLabel = new JLabel("Nick Name:");
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(nickNameLabel, gbc);

        nickNameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 5;
        panel.add(nickNameField, gbc);

        JLabel profilePictureLabel = new JLabel("Profile Picture:");
        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(profilePictureLabel, gbc);

        JPanel picturePanel = new JPanel(new BorderLayout());
        profilePictureField = new JTextField(15);
        profilePictureField.setEditable(false);
        profilePictureField.setText("default.png");
        profilePictureButton = new JButton("Browse...");
        picturePanel.add(profilePictureField, BorderLayout.CENTER);
        picturePanel.add(profilePictureButton, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.gridy = 6;
        panel.add(picturePanel, gbc);

        // Add action listener for profile picture button
        profilePictureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select Profile Picture");
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                    public boolean accept(java.io.File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".jpg") ||
                                f.getName().toLowerCase().endsWith(".jpeg") ||
                                f.getName().toLowerCase().endsWith(".png") ||
                                f.getName().toLowerCase().endsWith(".gif");
                    }
                    public String getDescription() {
                        return "Image Files (*.jpg, *.jpeg, *.png, *.gif)";
                    }
                });

                int result = fileChooser.showOpenDialog(Register.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    java.io.File selectedFile = fileChooser.getSelectedFile();
                    selectedProfilePicture = selectedFile.getName();
                    profilePictureField.setText(selectedProfilePicture);
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        registerButton = new JButton("Register");
        backButton = new JButton("Back to Login");
        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        statusLabel = new JLabel("", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        panel.add(statusLabel, gbc);

        add(panel);

        // Add action listeners
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                register();
            }
        });

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                backToLogin();
            }
        });
    }

    /**
     * Attempt to register a new user with the provided information.
     */
    private void register() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String email = emailField.getText().trim();
        String nickName = nickNameField.getText().trim();
        String profilePicture = selectedProfilePicture;

        // Validate input
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || email.isEmpty()) {
            statusLabel.setText("All fields are required");
            return;
        }

        // Nick name is optional, use username if not provided
        if (nickName.isEmpty()) {
            nickName = username;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match");
            return;
        }

        // Check if server is running, start it if not
        if (!ensureServerRunning()) {
            statusLabel.setText("Could not connect to server. Please try again later.");
            return;
        }

        try {
            Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
            UserService userService = (UserService) registry.lookup(USER_SERVICE_NAME);

            // Check if username is available
            if (!userService.isUsernameAvailable(username)) {
                statusLabel.setText("Username already taken");
                return;
            }

            // Register the user
            ChatUser user = userService.register(username, password, email, nickName, profilePicture);

            if (user != null) {
                JOptionPane.showMessageDialog(this,
                        "Registration successful! You can now log in.",
                        "Registration Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                backToLogin();
            } else {
                statusLabel.setText("Registration failed. Please try again.");
            }
        } catch (RemoteException | NotBoundException e) {
            statusLabel.setText("Error connecting to server: " + e.getMessage());
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
    }
}


