package com.example.chatapp.connect;

import com.example.chatapp.client.user.Login;
import com.example.chatapp.server.implementation.Server;

import javax.swing.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Main entry point for the chat application.
 * Starts the server if needed and launches the login interface.
 */
public class Demo {
    private static final int RMI_PORT = 1099;
    private static final String RMI_HOST = "localhost";
    private static final String USER_SERVICE_NAME = "UserService";

    /**
     * Main method to start the application.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Check if server is already running
        boolean serverRunning = isServerRunning();

        if (!serverRunning) {
            // Ask if user wants to start the server
            int response = JOptionPane.showConfirmDialog(
                    null,
                    "Server is not running. Would you like to start it?",
                    "Start Server",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (response == JOptionPane.YES_OPTION) {
                startServer();
            } else {
                JOptionPane.showMessageDialog(
                        null,
                        "Application cannot run without a server. Exiting.",
                        "Server Required",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
        }

        // Start the login interface
        SwingUtilities.invokeLater(() -> {
            new Login().setVisible(true);
        });
    }

    /**
     * Check if the server is already running.
     *
     * @return true if server is running, false otherwise
     */
    private static boolean isServerRunning() {
        try {
            Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
            registry.lookup(USER_SERVICE_NAME);
            return true;
        } catch (RemoteException | NotBoundException e) {
            return false;
        }
    }

    /**
     * Start the server in a separate thread.
     */
    private static void startServer() {
        Thread serverThread = new Thread(() -> {
            try {
                System.out.println("Starting server...");
                Server server = new Server();
                server.start();
                System.out.println("Server started successfully.");
            } catch (Exception e) {
                System.err.println("Error starting server: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Failed to start server: " + e.getMessage(),
                        "Server Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
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
            } catch (RemoteException | NotBoundException e) {
                retries++;
                System.out.println("Waiting for server... (" + retries + "/" + maxRetries + ")");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!serverStarted) {
            JOptionPane.showMessageDialog(
                    null,
                    "Server did not start within the expected time. Please try again.",
                    "Server Timeout",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }
    }
}

