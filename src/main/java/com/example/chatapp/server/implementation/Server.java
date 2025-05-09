import com.example.chatapp.domain.*;
import com.example.chatapp.rmi.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server implementation for the chat application.
 * Implements all RMI interfaces and manages the chat application.
 */
public class Server implements ChatService, UserService, SubscribeService, LogService {

    private static final int RMI_PORT = 1099;
    private static final String CHAT_SERVICE_NAME = "ChatService";
    private static final String USER_SERVICE_NAME = "UserService";
    private static final String SUBSCRIBE_SERVICE_NAME = "SubscribeService";
    private static final String LOG_SERVICE_NAME = "LogService";

    private final SessionFactory sessionFactory;
    private final Map<String, ChatObserver> observers = new ConcurrentHashMap<>();

    /**
     * Constructor initializes the Hibernate session factory and ensures logs directory exists.
     */
    public Server() {
        // Ensure logs directory exists
        ensureLogsDirectoryExists();

        // Ensure database exists
        ensureDatabaseExists();

        try {
            // Initialize Hibernate with detailed error handling
            System.out.println("Initializing Hibernate...");
            Configuration configuration = new Configuration().configure();
            System.out.println("Hibernate configuration loaded successfully.");

            // Log some configuration details for debugging
            System.out.println("Hibernate connection URL: " + configuration.getProperty("hibernate.connection.url"));
            System.out.println("Hibernate dialect: " + configuration.getProperty("hibernate.dialect"));

            // Build the session factory
            System.out.println("Building Hibernate SessionFactory...");
            sessionFactory = configuration.buildSessionFactory();
            System.out.println("Hibernate SessionFactory built successfully.");

            // Test the session factory by opening and closing a session
            System.out.println("Testing Hibernate session...");
            Session testSession = sessionFactory.openSession();
            testSession.close();
            System.out.println("Hibernate session test successful.");

            // Initialize database if needed
            initializeDatabase();
        } catch (Exception e) {
            System.err.println("ERROR: Failed to initialize Hibernate: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Hibernate. Application cannot start.", e);
        }
    }

    /**
     * Ensures that the logs directory exists, creating it if necessary.
     */
    private void ensureLogsDirectoryExists() {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                boolean created = logsDir.mkdirs();
                if (created) {
                    System.out.println("Logs directory created successfully.");
                } else {
                    System.err.println("Failed to create logs directory.");
                }
            } else {
                System.out.println("Logs directory already exists.");
            }
        } catch (Exception e) {
            System.err.println("Error ensuring logs directory exists: " + e.getMessage());
            e.printStackTrace();
        }
    }
