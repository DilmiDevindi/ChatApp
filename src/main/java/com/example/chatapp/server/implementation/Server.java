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
    /**
     * Main method to start the server.
     */
    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    // ChatService implementation

    @Override
    public ChatMsg sendMessage(String sender, String receiver, String message) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            ChatUser senderUser = getUserByUsername(sender);
            ChatUser receiverUser = getUserByUsername(receiver);

            if (senderUser == null || receiverUser == null) {
                return null;
            }

            // Check if this is a "Bye" message to leave the chat
            if (message.trim().equalsIgnoreCase("Bye")) {
                // User is leaving the chat
                ChatMsg chatMsg = new ChatMsg(senderUser, receiverUser, message);
                session.persist(chatMsg);
                transaction.commit();

                // Notify the receiver that the user has left
                notifyUserLeft(receiver, senderUser.getUsername(),
                        senderUser.getNickName() != null ? senderUser.getNickName() : senderUser.getUsername(),
                        new Date());

                return chatMsg;
            }

            ChatMsg chatMsg = new ChatMsg(senderUser, receiverUser, message);
            session.persist(chatMsg);

            transaction.commit();

            // Notify the receiver if online
            notifyUser(receiverUser.getUsername(), chatMsg);

            return chatMsg;
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ChatMsg sendGroupMessage(String sender, String groupName, String message) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            ChatUser senderUser = getUserByUsername(sender);

            Query<ChatGrp> query = session.createQuery("FROM ChatGrp WHERE name = :name", ChatGrp.class);
            query.setParameter("name", groupName);
            ChatGrp group = query.uniqueResult();

            if (senderUser == null || group == null) {
                return null;
            }

            // Check if sender is a member of the group
            if (!group.isMember(senderUser)) {
                return null;
            }

            // Check if this is a "Bye" message to leave the chat
            if (message.trim().equalsIgnoreCase("Bye")) {
                // User is leaving the chat
                notifyUserLeft(groupName, senderUser.getUsername(), senderUser.getNickName(), new Date());
                return null;
            }

            // Get the admin user
            Query<ChatUser> adminQuery = session.createQuery("FROM ChatUser WHERE isAdmin = true", ChatUser.class);
            ChatUser admin = adminQuery.uniqueResult();

            if (admin == null) {
                System.err.println("No admin found for subscription check");
                return null;
            }

            // Removed subscription check to allow all group members to send messages
            // Users who are members of a group should be able to send messages to that group
            // regardless of their subscription status with the admin

            // Create message with sender's profile information
            String formattedMessage = senderUser.getNickName() + " [" + senderUser.getUsername() + "]: " + message;
            ChatMsg chatMsg = new ChatMsg(senderUser, group, formattedMessage);
            session.persist(chatMsg);

            transaction.commit();

            // Notify all members of this specific group, including the sender
            for (ChatUser member : group.getMembers()) {
                notifyUser(member.getUsername(), chatMsg);
            }

            return chatMsg;
        } catch (Exception e) {
            System.err.println("Error sending group message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<ChatMsg> getMessages(String user1, String user2) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<ChatMsg> query = session.createQuery(
                    "FROM ChatMsg WHERE (sender.username = :user1 AND receiver.username = :user2) " +
                            "OR (sender.username = :user2 AND receiver.username = :user1) " +
                            "ORDER BY sentTime", ChatMsg.class);
            query.setParameter("user1", user1);
            query.setParameter("user2", user2);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting messages: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<ChatMsg> getGroupMessages(String groupName) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<ChatMsg> query = session.createQuery(
                    "FROM ChatMsg WHERE group.name = :groupName ORDER BY sentTime", ChatMsg.class);
            query.setParameter("groupName", groupName);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting group messages: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public ChatGrp createGroup(String groupName, String description, String creatorUsername) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            ChatUser creator = getUserByUsername(creatorUsername);

            if (creator == null) {
                return null;
            }

            // Check if group name is already taken
            Query<ChatGrp> query = session.createQuery("FROM ChatGrp WHERE name = :name", ChatGrp.class);
            query.setParameter("name", groupName);
            if (query.uniqueResult() != null) {
                return null;
            }

            ChatGrp group = new ChatGrp(groupName, description, creator);
            session.persist(group);

            transaction.commit();

            // Notify all observers that a new chat has started
            notifyChatStarted(groupName, new Date());

            return group;
        } catch (Exception e) {
            System.err.println("Error creating group: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean addUserToGroup(String groupName, String username) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            ChatUser user = getUserByUsername(username);

            Query<ChatGrp> query = session.createQuery("FROM ChatGrp WHERE name = :name", ChatGrp.class);
            query.setParameter("name", groupName);
            ChatGrp group = query.uniqueResult();

            if (user == null || group == null) {
                return false;
            }

            group.addMember(user);
            session.merge(group);

            transaction.commit();

            // Notify all observers that a user has joined the chat
            String nickName = user.getNickName() != null ? user.getNickName() : user.getUsername();
            notifyUserJoined(groupName, username, nickName, new Date());

            return true;
        } catch (Exception e) {
            System.err.println("Error adding user to group: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean removeUserFromGroup(String groupName, String username) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            ChatUser user = getUserByUsername(username);

            Query<ChatGrp> query = session.createQuery("FROM ChatGrp WHERE name = :name", ChatGrp.class);
            query.setParameter("name", groupName);
            ChatGrp group = query.uniqueResult();

            if (user == null || group == null) {
                return false;
            }

            // Cannot remove the creator
            if (group.getCreator().getUsername().equals(username)) {
                return false;
            }

            group.removeMember(user);
            session.merge(group);

            transaction.commit();

            // Notify all observers that a user has left the chat
            String nickName = user.getNickName() != null ? user.getNickName() : user.getUsername();
            notifyUserLeft(groupName, username, nickName, new Date());

            return true;
        } catch (Exception e) {
            System.err.println("Error removing user from group: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<ChatGrp> getAllGroups() throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<ChatGrp> query = session.createQuery("FROM ChatGrp", ChatGrp.class);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting all groups: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<ChatGrp> getUserGroups(String username) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<ChatGrp> query = session.createQuery(
                    "SELECT g FROM ChatGrp g JOIN g.members m WHERE m.username = :username", ChatGrp.class);
            query.setParameter("username", username);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting user groups: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public void registerObserver(ChatObserver observer) throws RemoteException {
        String username = observer.getUsername();
        observers.put(username, observer);
        System.out.println("Observer registered: " + username);

        // Notify all other users that this user is now online
        for (ChatObserver otherObserver : observers.values()) {
            if (!otherObserver.getUsername().equals(username)) {
                try {
                    otherObserver.userStatusChanged(username, true);
                } catch (RemoteException e) {
                    // Remove the observer if it's no longer reachable
                    observers.remove(otherObserver.getUsername());
                }
            }
        }
    }

    @Override
    public void unregisterObserver(ChatObserver observer) throws RemoteException {
        String username = observer.getUsername();
        observers.remove(username);
        System.out.println("Observer unregistered: " + username);

        // Notify all other users that this user is now offline
        for (ChatObserver otherObserver : observers.values()) {
            try {
                otherObserver.userStatusChanged(username, false);
            } catch (RemoteException e) {
                // Remove the observer if it's no longer reachable
                observers.remove(otherObserver.getUsername());
            }
        }
    }

    @Override
    public List<ChatUser> getOnlineUsers() throws RemoteException {
        List<ChatUser> onlineUsers = new ArrayList<>();

        try (Session session = sessionFactory.openSession()) {
            for (String username : observers.keySet()) {
                ChatUser user = getUserByUsername(username);
                if (user != null) {
                    onlineUsers.add(user);
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting online users: " + e.getMessage());
            e.printStackTrace();
        }

        return onlineUsers;
    }

    // UserService implementation

    @Override
    public ChatUser register(String username, String password, String email, String nickName, String profilePicture) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            // Check if username is already taken
            if (!isUsernameAvailable(username)) {
                return null;
            }

            ChatUser user = new ChatUser(username, password, email, nickName, profilePicture, false);
            session.persist(user);

            transaction.commit();

            return user;
        } catch (Exception e) {
            System.err.println("Error registering user: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ChatUser login(String username, String password) throws RemoteException {
        System.out.println("Login attempt: username=" + username);
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            // First check if the user exists
            Query<ChatUser> userQuery = session.createQuery(
                    "FROM ChatUser WHERE username = :username", ChatUser.class);
            userQuery.setParameter("username", username);
            ChatUser userCheck = userQuery.uniqueResult();

            if (userCheck == null) {
                System.out.println("Login failed: User not found with username: " + username);
                return null;
            }

            System.out.println("User found: " + userCheck.getUsername() + ", isAdmin=" + userCheck.isAdmin());

            // Now check password
            Query<ChatUser> query = session.createQuery(
                    "FROM ChatUser WHERE username = :username AND password = :password", ChatUser.class);
            query.setParameter("username", username);
            query.setParameter("password", password);
            ChatUser user = query.uniqueResult();

            if (user != null) {
                System.out.println("Login successful for user: " + user.getUsername() + ", isAdmin=" + user.isAdmin());

                // Special handling for admin users
                if (user.isAdmin()) {
                    System.out.println("Admin user logging in, ensuring proper state");
                    // Force the online status to false first to ensure clean state
                    user.setOnline(false);
                    session.merge(user);
                    session.flush();

                    // Now set to online
                    user.setOnline(true);
                    session.merge(user);
                } else {
                    // Regular user login
                    user.setOnline(true);
                    session.merge(user);
                }

                // Log the login
                com.example.chatapp.domain.ChatLogs log = new com.example.chatapp.domain.ChatLogs(user, "LOGIN", null, "User logged in");
                session.persist(log);

                System.out.println("User login completed successfully: " + user.getUsername());
            } else {
                System.out.println("Login failed: Incorrect password for user: " + username);
            }

            transaction.commit();

            return user;
        } catch (Exception e) {
            System.err.println("Error logging in user: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean logout(String username) throws RemoteException {
        System.out.println("Logout request for user: " + username);
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            ChatUser user = getUserByUsername(username);

            if (user == null) {
                System.err.println("Logout failed: User not found: " + username);
                return false;
            }

            System.out.println("Found user for logout: " + user.getUsername() + ", isAdmin=" + user.isAdmin());
            user.setOnline(false);
            session.merge(user);

            // Log the logout
            com.example.chatapp.domain.ChatLogs log = new com.example.chatapp.domain.ChatLogs(user, "LOGOUT", null, "User logged out");
            session.persist(log);

            transaction.commit();
            System.out.println("User successfully logged out: " + username);

            // If this is an admin user, make sure to unregister any observers
            if (user.isAdmin()) {
                System.out.println("Admin user logged out, cleaning up resources");
                // Additional cleanup for admin users if needed
            }

            return true;
        } catch (Exception e) {
            System.err.println("Error logging out user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public ChatUser getUserByUsername(String username) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<ChatUser> query = session.createQuery(
                    "FROM ChatUser WHERE username = :username", ChatUser.class);
            query.setParameter("username", username);
            return query.uniqueResult();
        } catch (Exception e) {
            System.err.println("Error getting user by username: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ChatUser updateProfile(String username, String newPassword, String newEmail, String newNickName, String newProfilePicture) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            ChatUser user = getUserByUsername(username);

            if (user == null) {
                return null;
            }

            if (newPassword != null && !newPassword.isEmpty()) {
                user.setPassword(newPassword);
            }

            if (newNickName != null && !newNickName.isEmpty()) {
                user.setNickName(newNickName);
            }

            if (newProfilePicture != null && !newProfilePicture.isEmpty()) {
                user.setProfilePicture(newProfilePicture);
            }

            session.merge(user);

            transaction.commit();

            return user;
        } catch (Exception e) {
            System.err.println("Error updating profile: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<ChatUser> getAllUsers() throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<ChatUser> query = session.createQuery("FROM ChatUser", ChatUser.class);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting all users: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isUsernameAvailable(String username) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(u) FROM ChatUser u WHERE u.username = :username", Long.class);
            query.setParameter("username", username);
            return query.uniqueResult() == 0;
        } catch (Exception e) {
            System.err.println("Error checking username availability: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public ChatUser createAdmin(String username, String password, String email) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            // Check if an admin already exists
            Query<Long> adminQuery = session.createQuery(
                    "SELECT COUNT(u) FROM ChatUser u WHERE u.isAdmin = true", Long.class);
            if (adminQuery.uniqueResult() > 0) {
                return null;
            }

            // Check if username is already taken
            if (!isUsernameAvailable(username)) {
                return null;
            }

            ChatUser admin = new ChatUser(username, password, email, true);
            session.persist(admin);

            transaction.commit();

            return admin;
        } catch (Exception e) {
            System.err.println("Error creating admin: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean isAdmin(String username) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<ChatUser> query = session.createQuery(
                    "FROM ChatUser WHERE username = :username AND isAdmin = true", ChatUser.class);
            query.setParameter("username", username);
            return query.uniqueResult() != null;
        } catch (Exception e) {
            System.err.println("Error checking if user is admin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean removeUser(String adminUsername, String userToRemove) throws RemoteException {
        // Check if the user performing the operation is an admin
        if (!isAdmin(adminUsername)) {
            System.err.println("User " + adminUsername + " is not an admin. Cannot remove users.");
            return false;
        }

        // Cannot remove yourself
        if (adminUsername.equals(userToRemove)) {
            System.err.println("Admin cannot remove themselves.");
            return false;
        }

        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            // Get the user to be removed
            ChatUser user = getUserByUsername(userToRemove);
            if (user == null) {
                System.err.println("User " + userToRemove + " not found.");
                return false;
            }

            // Check if the user to be removed is an admin
            if (user.isAdmin()) {
                System.err.println("Cannot remove admin user " + userToRemove);
                return false;
            }

            // Remove user from all groups
            Query<ChatGrp> groupQuery = session.createQuery(
                    "FROM ChatGrp g WHERE :user MEMBER OF g.members", ChatGrp.class);
            groupQuery.setParameter("user", user);
            List<ChatGrp> groups = groupQuery.list();

            for (ChatGrp group : groups) {
                group.removeMember(user);
                session.merge(group);
            }

            // Remove all subscriptions where the user is a subscriber or target
            Query<Subscribe> subQuery = session.createQuery(
                    "FROM Subscribe s WHERE s.subscriber = :user OR s.target = :user", Subscribe.class);
            subQuery.setParameter("user", user);
            List<Subscribe> subscriptions = subQuery.list();

            for (Subscribe subscription : subscriptions) {
                session.remove(subscription);
            }

            // Remove the user
            session.remove(user);

            // Log the action
            ChatUser admin = getUserByUsername(adminUsername);
            com.example.chatapp.domain.ChatLogs log = new com.example.chatapp.domain.ChatLogs(admin, "REMOVE_USER", null, "Admin " + adminUsername + " removed user " + userToRemove);
            session.persist(log);

            transaction.commit();

            System.out.println("User " + userToRemove + " successfully removed by admin " + adminUsername);
            return true;
        } catch (Exception e) {
            System.err.println("Error removing user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // SubscribeService implementation

    @Override
    public Subscribe subscribe(String subscriberUsername, String targetUsername) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            ChatUser subscriber = getUserByUsername(subscriberUsername);
            ChatUser target = getUserByUsername(targetUsername);

            if (subscriber == null || target == null) {
                return null;
            }

            // Check if already subscribed
            if (isSubscribed(subscriberUsername, targetUsername)) {
                return null;
            }

            Subscribe subscription = new Subscribe(subscriber, target);
            session.persist(subscription);

            transaction.commit();

            return subscription;
        } catch (Exception e) {
            System.err.println("Error subscribing: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean unsubscribe(String subscriberUsername, String targetUsername) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            Query<Subscribe> query = session.createQuery(
                    "FROM Subscribe WHERE subscriber.username = :subscriber AND target.username = :target", Subscribe.class);
            query.setParameter("subscriber", subscriberUsername);
            query.setParameter("target", targetUsername);
            Subscribe subscription = query.uniqueResult();

            if (subscription == null) {
                return false;
            }

            session.remove(subscription);

            transaction.commit();

            return true;
        } catch (Exception e) {
            System.err.println("Error unsubscribing: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Subscribe> getSubscriptions(String username) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<Subscribe> query = session.createQuery(
                    "FROM Subscribe WHERE subscriber.username = :username", Subscribe.class);
            query.setParameter("username", username);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting subscriptions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<ChatUser> getSubscribers(String username) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<ChatUser> query = session.createQuery(
                    "SELECT s.subscriber FROM Subscribe s WHERE s.target.username = :username", ChatUser.class);
            query.setParameter("username", username);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting subscribers: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isSubscribed(String subscriberUsername, String targetUsername) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(s) FROM Subscribe s WHERE s.subscriber.username = :subscriber AND s.target.username = :target", Long.class);
            query.setParameter("subscriber", subscriberUsername);
            query.setParameter("target", targetUsername);
            return query.uniqueResult() > 0;
        } catch (Exception e) {
            System.err.println("Error checking if subscribed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<ChatUser> getSubscribedUsers(String username) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<ChatUser> query = session.createQuery(
                    "SELECT s.target FROM Subscribe s WHERE s.subscriber.username = :username", ChatUser.class);
            query.setParameter("username", username);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting subscribed users: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ChatService notification methods

    @Override
    public void notifyChatStarted(String chatName, Date startTime) throws RemoteException {
        System.out.println("Chat started: " + chatName + " at " + startTime);

        try (Session session = sessionFactory.openSession()) {
            // Get the admin user who started the chat
            Query<ChatUser> adminQuery = session.createQuery("FROM ChatUser WHERE isAdmin = true", ChatUser.class);
            ChatUser admin = adminQuery.uniqueResult();

            if (admin == null) {
                System.err.println("No admin found to start chat");
                return;
            }

            // Get the group
            Query<ChatGrp> groupQuery = session.createQuery("FROM ChatGrp WHERE name = :name", ChatGrp.class);
            groupQuery.setParameter("name", chatName);
            ChatGrp group = groupQuery.uniqueResult();

            if (group == null) {
                System.err.println("Group not found: " + chatName);
                return;
            }

            // Notify all online members of the group
            for (ChatUser member : group.getMembers()) {
                ChatObserver observer = observers.get(member.getUsername());
                if (observer != null) {
                    try {
                        observer.chatStarted(chatName, startTime);
                    } catch (RemoteException e) {
                        // Remove the observer if it's no longer reachable
                        observers.remove(member.getUsername());
                        System.err.println("Error notifying user about chat start: " + e.getMessage());
                    }
                }
            }

            // Log the chat start
            com.example.chatapp.domain.ChatLogs log = new com.example.chatapp.domain.ChatLogs(
                    admin, "CHAT_STARTED", null, "Admin started chat: " + chatName);
            Transaction transaction = session.beginTransaction();
            session.persist(log);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("Error notifying chat started: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void notifyUserJoined(String chatName, String username, String nickName, Date joinTime) throws RemoteException {
        System.out.println("User joined: " + username + " (" + nickName + ") to chat: " + chatName + " at " + joinTime);

        try (Session session = sessionFactory.openSession()) {
            ChatUser user = getUserByUsername(username);

            if (user == null) {
                System.err.println("User not found: " + username);
                return;
            }

            // Get the group
            Query<ChatGrp> groupQuery = session.createQuery("FROM ChatGrp WHERE name = :name", ChatGrp.class);
            groupQuery.setParameter("name", chatName);
            ChatGrp group = groupQuery.uniqueResult();

            if (group == null) {
                System.err.println("Group not found: " + chatName);
                return;
            }

            // Notify all online members of the group
            for (ChatUser member : group.getMembers()) {
                // Don't notify the user who joined
                if (member.getUsername().equals(username)) {
                    continue;
                }

                ChatObserver observer = observers.get(member.getUsername());
                if (observer != null) {
                    try {
                        observer.userJoined(chatName, username, nickName, joinTime);
                    } catch (RemoteException e) {
                        // Remove the observer if it's no longer reachable
                        observers.remove(member.getUsername());
                        System.err.println("Error notifying user about join: " + e.getMessage());
                    }
                }
            }

            // Log the user join
            com.example.chatapp.domain.ChatLogs log = new com.example.chatapp.domain.ChatLogs(
                    user, "USER_JOINED_CHAT", null, "User joined chat: " + chatName);
            Transaction transaction = session.beginTransaction();
            session.persist(log);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("Error notifying user joined: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void notifyUserLeft(String chatName, String username, String nickName, Date leaveTime) throws RemoteException {
        System.out.println("User left: " + username + " (" + nickName + ") from chat: " + chatName + " at " + leaveTime);

        try (Session session = sessionFactory.openSession()) {
            ChatUser user = getUserByUsername(username);

            if (user == null) {
                System.err.println("User not found: " + username);
                return;
            }

            // Get the group
            Query<ChatGrp> groupQuery = session.createQuery("FROM ChatGrp WHERE name = :name", ChatGrp.class);
            groupQuery.setParameter("name", chatName);
            ChatGrp group = groupQuery.uniqueResult();

            if (group == null) {
                System.err.println("Group not found: " + chatName);
                return;
            }

            // Notify all online members of the group
            for (ChatUser member : group.getMembers()) {
                // Don't notify the user who left
                if (member.getUsername().equals(username)) {
                    continue;
                }

                ChatObserver observer = observers.get(member.getUsername());
                if (observer != null) {
                    try {
                        observer.userLeft(chatName, username, nickName, leaveTime);
                    } catch (RemoteException e) {
                        // Remove the observer if it's no longer reachable
                        observers.remove(member.getUsername());
                        System.err.println("Error notifying user about leave: " + e.getMessage());
                    }
                }
            }

            // Log the user leave
            com.example.chatapp.domain.ChatLogs log = new com.example.chatapp.domain.ChatLogs(
                    user, "USER_LEFT_CHAT", null, "User left chat: " + chatName);
            Transaction transaction = session.beginTransaction();
            session.persist(log);
            transaction.commit();

            // Check if this was the last user in the group
            Query<Long> countQuery = session.createQuery(
                    "SELECT COUNT(*) FROM ChatGrp g JOIN g.members m WHERE g.name = :groupName",
                    Long.class);
            countQuery.setParameter("groupName", chatName);
            Long userCount = countQuery.uniqueResult();

            if (userCount != null && userCount == 0) {
                // This was the last user, stop the chat
                notifyChatStopped(chatName, new Date());
            }
        } catch (Exception e) {
            System.err.println("Error notifying user left: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void notifyChatStopped(String chatName, Date stopTime) throws RemoteException {
        System.out.println("Chat stopped: " + chatName + " at " + stopTime);

        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            // Get the admin user
            Query<ChatUser> adminQuery = session.createQuery("FROM ChatUser WHERE isAdmin = true", ChatUser.class);
            ChatUser admin = adminQuery.uniqueResult();

            if (admin == null) {
                System.err.println("No admin found to log chat stop");
                return;
            }

            // Generate a unique chat ID
            String chatId = UUID.randomUUID().toString();

            // Get all messages for this chat
            List<ChatMsg> messages = getGroupMessages(chatName);

            // Save chat logs to a file using the FileWriter utility
            String filePath;
            try {
                filePath = com.example.chatapp.util.FileWriter.saveChatToFile(chatName, chatId, messages, stopTime);
                System.out.println("Chat logs saved to: " + filePath);
            } catch (IOException e) {
                System.err.println("Error saving chat logs to file: " + e.getMessage());
                e.printStackTrace();
                filePath = "Error saving file";
            }

            // Create and save a ChatRecord in the database
            com.example.chatapp.domain.ChatRecord chatRecord = new com.example.chatapp.domain.ChatRecord(
                    chatId, chatName, filePath, stopTime);
            session.persist(chatRecord);
            System.out.println("Chat record saved to database with ID: " + chatRecord.getId());

            // Get the group
            Query<ChatGrp> groupQuery = session.createQuery("FROM ChatGrp WHERE name = :name", ChatGrp.class);
            groupQuery.setParameter("name", chatName);
            ChatGrp group = groupQuery.uniqueResult();

            if (group != null) {
                // Notify all online members of the group
                for (ChatUser member : group.getMembers()) {
                    ChatObserver observer = observers.get(member.getUsername());
                    if (observer != null) {
                        try {
                            observer.chatStopped(chatName, stopTime);
                        } catch (RemoteException e) {
                            // Remove the observer if it's no longer reachable
                            observers.remove(member.getUsername());
                            System.err.println("Error notifying user about chat stop: " + e.getMessage());
                        }
                    }
                }
            } else {
                System.err.println("Group not found: " + chatName);
            }

            // Log the chat stop
            com.example.chatapp.domain.ChatLogs log = new com.example.chatapp.domain.ChatLogs(
                    admin, "CHAT_STOPPED", null, "Chat stopped: " + chatName, chatId, filePath);
            session.persist(log);

            transaction.commit();
        } catch (Exception e) {
            System.err.println("Error notifying chat stopped: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is no longer used. Chat logs are now saved using the FileWriter utility class.
     * @deprecated Use {@link com.example.chatapp.util.FileWriter#saveChatToFile} instead.
     */
    @Override
    @Deprecated
    public String saveChatLogs(String chatName, String chatId) throws RemoteException {
        // This method is kept for backward compatibility with the interface
        // but its implementation is now delegated to the FileWriter utility
        System.out.println("Deprecated method called: saveChatLogs. Using FileWriter utility instead.");

        try {
            List<ChatMsg> messages = getGroupMessages(chatName);
            return com.example.chatapp.util.FileWriter.saveChatToFile(chatName, chatId, messages, new Date());
        } catch (Exception e) {
            System.err.println("Error in deprecated saveChatLogs method: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Helper methods

    /**
     * Notify a user about a new message.
     */
    private void notifyUser(String username, ChatMsg message) {
        ChatObserver observer = observers.get(username);
        if (observer != null) {
            try {
                observer.update(message);
            } catch (RemoteException e) {
                // Remove the observer if it's no longer reachable
                observers.remove(username);
                System.err.println("Error notifying user: " + e.getMessage());
            }
        }
    }

    // LogService implementation

    @Override
    public List<com.example.chatapp.domain.ChatLogs> getAllLogs() throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<com.example.chatapp.domain.ChatLogs> query = session.createQuery(
                    "FROM ChatLogs ORDER BY timestamp DESC", com.example.chatapp.domain.ChatLogs.class);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting all logs: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<com.example.chatapp.domain.ChatLogs> getUserLogs(String username) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<com.example.chatapp.domain.ChatLogs> query = session.createQuery(
                    "FROM ChatLogs WHERE user.username = :username ORDER BY timestamp DESC",
                    com.example.chatapp.domain.ChatLogs.class);
            query.setParameter("username", username);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting user logs: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<com.example.chatapp.domain.ChatLogs> getLogsByAction(String action) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<com.example.chatapp.domain.ChatLogs> query = session.createQuery(
                    "FROM ChatLogs WHERE action = :action ORDER BY timestamp DESC",
                    com.example.chatapp.domain.ChatLogs.class);
            query.setParameter("action", action);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting logs by action: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<com.example.chatapp.domain.ChatLogs> getLogsByDateRange(Date startDate, Date endDate) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Query<com.example.chatapp.domain.ChatLogs> query = session.createQuery(
                    "FROM ChatLogs WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC",
                    com.example.chatapp.domain.ChatLogs.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting logs by date range: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public boolean clearLogs() throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            Query<?> query = session.createQuery("DELETE FROM ChatLogs");
            int deletedCount = query.executeUpdate();

            transaction.commit();

            System.out.println("Cleared " + deletedCount + " logs");
            return true;
        } catch (Exception e) {
            System.err.println("Error clearing logs: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
