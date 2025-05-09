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

    /**
     * Ensures that the database exists, creating it if necessary.
     * Also tests the MySQL connection and provides diagnostic information.
     */
    private void ensureDatabaseExists() {
        String jdbcUrl = "jdbc:mysql://localhost:3306/";
        String username = "root";
        String password = "1234";
        String dbName = "chatdb";

        try {
            // Load the MySQL JDBC driver explicitly
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded successfully.");

            // Test connection to MySQL server
            System.out.println("Attempting to connect to MySQL server at " + jdbcUrl);
            java.sql.Connection connection = java.sql.DriverManager.getConnection(jdbcUrl, username, password);
            System.out.println("Connected to MySQL server successfully.");

            java.sql.Statement statement = connection.createStatement();

            // Create database if it doesn't exist
            System.out.println("Checking if database '" + dbName + "' exists...");
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
            System.out.println("Database '" + dbName + "' ensured.");

            // Test connection to the specific database
            String fullJdbcUrl = jdbcUrl + dbName;
            System.out.println("Testing connection to database: " + fullJdbcUrl);
            java.sql.Connection dbConnection = java.sql.DriverManager.getConnection(fullJdbcUrl, username, password);
            System.out.println("Successfully connected to database: " + dbName);
            dbConnection.close();

            // Close resources
            statement.close();
            connection.close();
            System.out.println("Database connection test completed successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR: MySQL JDBC Driver not found. Please ensure the MySQL connector is in the classpath.");
            System.err.println("Error details: " + e.getMessage());
            e.printStackTrace();
        } catch (java.sql.SQLException e) {
            System.err.println("ERROR: Database connection failed.");
            System.err.println("Error code: " + e.getErrorCode());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Please ensure MySQL server is running on localhost:3306 and credentials are correct.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("ERROR: Unexpected error ensuring database exists: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize the database with an admin user if none exists.
     * Also ensures that the chat_records table is created.
     */
    private void initializeDatabase() {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            // Check if admin user exists
            Query<ChatUser> query = session.createQuery("FROM ChatUser WHERE isAdmin = true", ChatUser.class);
            List<ChatUser> admins = query.list();

            if (admins.isEmpty()) {
                // Create admin user
                ChatUser admin = new ChatUser("admin", "admin123", "admin@example.com", true);
                session.persist(admin);
                System.out.println("Admin user created: " + admin.getUsername());
            }

            // Ensure chat_records table is created by checking if it exists
            try {
                Query<Long> recordQuery = session.createQuery(
                        "SELECT COUNT(c) FROM ChatRecord c", Long.class);
                recordQuery.uniqueResult();
                System.out.println("chat_records table exists");
            } catch (Exception e) {
                System.out.println("Creating chat_records table...");
                // Create a test record to ensure the table is created
                String testChatId = "test-" + UUID.randomUUID().toString();
                ChatRecord testRecord = new ChatRecord(testChatId, "Test Chat", "test-path", new Date());
                session.persist(testRecord);
                System.out.println("Test chat record created with ID: " + testRecord.getId());

                // Remove the test record
                session.remove(testRecord);
                System.out.println("Test chat record removed");
            }

            transaction.commit();
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Start the RMI server.
     */
    public void start() {
        try {
            // Create and export remote objects
            ChatService chatService = (ChatService) UnicastRemoteObject.exportObject(this, 0);
            UserService userService = (UserService) chatService; // no need to export again
            SubscribeService subscribeService = (SubscribeService) chatService;
            LogService logService = (LogService) chatService;

            // Create or get the registry
            Registry registry = null;
            try {
                registry = LocateRegistry.createRegistry(RMI_PORT);
                System.out.println("RMI registry created on port " + RMI_PORT);
            } catch (RemoteException e) {
                registry = LocateRegistry.getRegistry(RMI_PORT);
                System.out.println("RMI registry found on port " + RMI_PORT);
            }

            // Bind the remote objects
            registry.rebind(CHAT_SERVICE_NAME, chatService);
            registry.rebind(USER_SERVICE_NAME, userService);
            registry.rebind(SUBSCRIBE_SERVICE_NAME, subscribeService);
            registry.rebind(LOG_SERVICE_NAME, logService);

            System.out.println("Server started. Services registered:");
            System.out.println("- " + CHAT_SERVICE_NAME);
            System.out.println("- " + USER_SERVICE_NAME);
            System.out.println("- " + SUBSCRIBE_SERVICE_NAME);
            System.out.println("- " + LOG_SERVICE_NAME);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
