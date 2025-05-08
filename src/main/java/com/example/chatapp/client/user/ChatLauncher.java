package com.example.chatapp.client.user;

import com.example.chatapp.domain.ChatGrp;
import com.example.chatapp.domain.ChatMsg;
import com.example.chatapp.domain.ChatUser;
import com.example.chatapp.domain.Subscribe;
import com.example.chatapp.domain.ChatLogs;
import com.example.chatapp.rmi.ChatObserver;
import com.example.chatapp.rmi.ChatService;
import com.example.chatapp.rmi.SubscribeService;
import com.example.chatapp.rmi.UserService;
import com.example.chatapp.rmi.LogService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main chat interface for regular users.
 * Allows sending messages, viewing conversations, and managing groups.
 */
public class ChatLauncher extends JFrame implements ChatObserver {
    private final ChatUser currentUser;
    private ChatService chatService;
    private UserService userService;
    private LogService logService;

    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JList<String> groupList;
    private DefaultListModel<String> groupListModel;
    private JEditorPane chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton createGroupButton;
    private JButton refreshButton;
    private JButton logoutButton;
    private JButton joinGroupButton;
    private JButton leaveGroupButton;

    // Map to store chat areas for each conversation
    private Map<String, JEditorPane> chatAreas = new HashMap<>();
    private JPanel chatCardPanel;
    private JEditorPane allGroupsChatArea;
    private JButton updateProfileButton;
    private JButton subscribeButton;
    private JButton unsubscribeButton;
    private JButton viewMembersButton;

    private String selectedUser;
    private String selectedGroup;
    private boolean isGroupSelected = false;

    private static final String CHAT_SERVICE_NAME = "ChatService";
    private static final String USER_SERVICE_NAME = "UserService";
    private static final String LOG_SERVICE_NAME = "LogService";
    private static final int RMI_PORT = 1099;
    private static final String RMI_HOST = "localhost";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("h.mm a");

    /**
     * Constructor initializes the chat UI.
     *
     * @param user The logged-in user
     */
    public ChatLauncher(ChatUser user) {
        this.currentUser = user;

        setTitle("Chat Application - " + user.getUsername());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize RMI services
        try {
            Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
            chatService = (ChatService) registry.lookup(CHAT_SERVICE_NAME);
            userService = (UserService) registry.lookup(USER_SERVICE_NAME);
            logService = (LogService) registry.lookup(LOG_SERVICE_NAME);

            // Export this object as a remote object
            UnicastRemoteObject.exportObject(this, 0);

            // Register as an observer
            chatService.registerObserver(this);
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
        loadGroups();

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
        // Define color scheme
        Color primaryColor = new Color(52, 73, 85);      // #344955 - Dark blue
        Color primaryLightColor = new Color(74, 101, 114); // #4A6572 - Medium blue
        Color primaryDarkColor = new Color(35, 47, 52);   // #232F34 - Very dark blue
        Color secondaryColor = new Color(249, 170, 51);   // #F9AA33 - Orange
        Color backgroundColor = new Color(245, 245, 250);  // Light background
        Color textColor = new Color(33, 33, 33);          // Dark text

        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(backgroundColor);

        // Left panel for users and groups
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, getHeight()));
        leftPanel.setBackground(backgroundColor);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Add "All Groups" button at the top of the left panel
        JButton allGroupsButton = new JButton("All Groups Chat");
        styleButton(allGroupsButton, secondaryColor, primaryDarkColor);
        allGroupsButton.addActionListener(e -> {
            selectedGroup = "All Groups";
            selectedUser = null;
            isGroupSelected = true;
            loadAllGroupMessages();
        });

        JPanel allGroupsPanel = new JPanel(new BorderLayout());
        allGroupsPanel.setBackground(backgroundColor);
        allGroupsPanel.add(allGroupsButton, BorderLayout.CENTER);
        allGroupsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        leftPanel.add(allGroupsPanel, BorderLayout.NORTH);

        // Users panel
        JPanel usersPanel = new JPanel(new BorderLayout());
        usersPanel.setBackground(backgroundColor);
        usersPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(primaryColor, 1),
                "Users",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 12),
                primaryColor));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBackground(backgroundColor);
        userList.setForeground(textColor);
        userList.setSelectionBackground(primaryLightColor);
        userList.setSelectionForeground(Color.WHITE);
        userList.setFont(new Font("Arial", Font.PLAIN, 12));

        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(BorderFactory.createEmptyBorder());
        usersPanel.add(userScrollPane, BorderLayout.CENTER);

        // User action buttons
        JPanel userButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        userButtonPanel.setBackground(backgroundColor);

        subscribeButton = new JButton("Subscribe");
        styleButton(subscribeButton, primaryColor, Color.WHITE);

        unsubscribeButton = new JButton("Unsubscribe");
        styleButton(unsubscribeButton, primaryColor, Color.WHITE);

        userButtonPanel.add(subscribeButton);
        userButtonPanel.add(unsubscribeButton);
        usersPanel.add(userButtonPanel, BorderLayout.SOUTH);

        // Groups panel
        JPanel groupsPanel = new JPanel(new BorderLayout());
        groupsPanel.setBackground(backgroundColor);
        groupsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(primaryColor, 1),
                "Groups",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 12),
                primaryColor));

        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setBackground(backgroundColor);
        groupList.setForeground(textColor);
        groupList.setSelectionBackground(primaryLightColor);
        groupList.setSelectionForeground(Color.WHITE);
        groupList.setFont(new Font("Arial", Font.PLAIN, 12));

        JScrollPane groupScrollPane = new JScrollPane(groupList);
        groupScrollPane.setBorder(BorderFactory.createEmptyBorder());
        groupsPanel.add(groupScrollPane, BorderLayout.CENTER);

        // Button panel for left side
        JPanel leftButtonPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        leftButtonPanel.setBackground(backgroundColor);

        createGroupButton = new JButton("Create Group");
        styleButton(createGroupButton, secondaryColor, primaryDarkColor);

        joinGroupButton = new JButton("Join Group");
        styleButton(joinGroupButton, secondaryColor, primaryDarkColor);

        leaveGroupButton = new JButton("Leave Group");
        styleButton(leaveGroupButton, secondaryColor, primaryDarkColor);

        refreshButton = new JButton("Refresh");
        styleButton(refreshButton, secondaryColor, primaryDarkColor);

        JButton viewMembersButton = new JButton("View Members");
        styleButton(viewMembersButton, secondaryColor, primaryDarkColor);

        leftButtonPanel.add(createGroupButton);
        leftButtonPanel.add(joinGroupButton);
        leftButtonPanel.add(leaveGroupButton);
        leftButtonPanel.add(refreshButton);
        leftButtonPanel.add(viewMembersButton);

        // Store the button as a class field so we can reference it later
        this.viewMembersButton = viewMembersButton;

        // Add components to left panel
        leftPanel.add(usersPanel, BorderLayout.NORTH);
        leftPanel.add(groupsPanel, BorderLayout.CENTER);
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);

        // Center panel for chat
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(backgroundColor);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create a panel with CardLayout to hold different chat areas
        chatCardPanel = new JPanel(new CardLayout());
        chatCardPanel.setBackground(backgroundColor);

        // Create default chat area
        chatArea = new JEditorPane("text/html", "");
        chatArea.setEditable(false);
        chatArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        chatArea.setBackground(backgroundColor);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 12));

        // Add default chat area to the card panel
        JScrollPane defaultScrollPane = new JScrollPane(chatArea);
        defaultScrollPane.setBorder(BorderFactory.createLineBorder(primaryLightColor, 1));
        chatCardPanel.add(defaultScrollPane, "default");

        // Add card panel to chat panel
        chatPanel.add(chatCardPanel, BorderLayout.CENTER);

        // Bottom panel for message input
        JPanel messagePanel = new JPanel(new BorderLayout(5, 0));
        messagePanel.setBackground(backgroundColor);
        messagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 12));
        messageField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primaryLightColor, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        sendButton = new JButton("Send");
        styleButton(sendButton, secondaryColor, primaryDarkColor);

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        // Top panel for user actions
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setBackground(primaryColor);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        updateProfileButton = new JButton("Update Profile");
        styleButton(updateProfileButton, secondaryColor, primaryDarkColor);

        logoutButton = new JButton("Logout");
        styleButton(logoutButton, secondaryColor, primaryDarkColor);

        topPanel.add(updateProfileButton);
        topPanel.add(logoutButton);

        // Add components to main panel
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(chatPanel, BorderLayout.CENTER);
        mainPanel.add(messagePanel, BorderLayout.SOUTH);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Add main panel to frame
        add(mainPanel);

        // Add action listeners
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && userList.getSelectedValue() != null) {
                selectedUser = userList.getSelectedValue();
                selectedGroup = null;
                isGroupSelected = false;
                loadMessages();
            }
        });

        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && groupList.getSelectedValue() != null) {
                selectedGroup = groupList.getSelectedValue();
                selectedUser = null;
                isGroupSelected = true;
                loadMessages();
            }
        });
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        createGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createGroup();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadUsers();
                loadGroups();
            }
        });

        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });

        joinGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                joinGroup();
            }
        });

        leaveGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leaveGroup();
            }
        });

        updateProfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateProfile();
            }
        });

        subscribeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                subscribeToUser();
            }
        });

        unsubscribeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unsubscribeFromUser();
            }
        });

        viewMembersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewGroupMembers();
            }
        });
    }

    /**
     * View members of a group without selecting it.
     */
    private void viewGroupMembers() {
        try {
            // Get groups the user is a member of
            List<ChatGrp> userGroups = chatService.getUserGroups(currentUser.getUsername());

            if (userGroups.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "You are not a member of any group.",
                        "No Groups",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Create a list of group names
            List<String> groupNames = userGroups.stream()
                    .map(ChatGrp::getName)
                    .toList();

            // Show dialog to select a group
            String selectedGroupName = (String) JOptionPane.showInputDialog(
                    this,
                    "Select a group to view members:",
                    "View Group Members",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    groupNames.toArray(),
                    groupNames.get(0)
            );

            if (selectedGroupName != null) {
                // Find the selected group
                ChatGrp selectedGroup = userGroups.stream()
                        .filter(g -> g.getName().equals(selectedGroupName))
                        .findFirst()
                        .orElse(null);

                if (selectedGroup != null) {
                    // Build a string with all members
                    StringBuilder membersInfo = new StringBuilder();
                    membersInfo.append("<html><body style='font-family: Arial, sans-serif; margin: 10px;'>");
                    membersInfo.append("<h2>").append(selectedGroupName).append(" - Group Members</h2>");

                    // Get the creation date
                    String startTime = DATE_FORMAT.format(selectedGroup.getCreatedDate());
                    membersInfo.append("<div style='color: #4A6572; font-weight: bold; margin-bottom: 10px;'>");
                    membersInfo.append("Group created at: ").append(startTime);
                    membersInfo.append("</div>");

                    // List all members
                    for (ChatUser member : selectedGroup.getMembers()) {
                        String memberNickName = member.getNickName() != null ? member.getNickName() : member.getUsername();
                        membersInfo.append("<div style='margin-left: 15px; margin-bottom: 5px;'>");

                        // Always use default profile icon instead of images
                        String memberNickNameInitial = memberNickName.substring(0, 1).toUpperCase();
                        membersInfo.append("<span style='display: inline-block; width: 30px; height: 30px; background-color: #344955; color: white; text-align: center; line-height: 30px; border-radius: 50%; margin-right: 5px;'>")
                                .append(memberNickNameInitial)
                                .append("</span>");

                        // If this member is the creator, show that
                        if (member.getUsername().equals(selectedGroup.getCreator().getUsername())) {
                            membersInfo.append(memberNickName).append(" (Creator)");
                        } else {
                            membersInfo.append(memberNickName);
                        }

                        membersInfo.append("</div>");
                    }

                    membersInfo.append("</body></html>");

                    // Create a custom dialog to display the members
                    JDialog dialog = new JDialog(this, "Group Members", true);
                    dialog.setLayout(new BorderLayout());

                    JEditorPane membersPane = new JEditorPane("text/html", membersInfo.toString());
                    membersPane.setEditable(false);
                    membersPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                    membersPane.setBackground(new Color(245, 245, 250));

                    JScrollPane scrollPane = new JScrollPane(membersPane);
                    dialog.add(scrollPane, BorderLayout.CENTER);

                    JButton closeButton = new JButton("Close");
                    closeButton.addActionListener(e -> dialog.dispose());

                    JPanel buttonPanel = new JPanel();
                    buttonPanel.add(closeButton);
                    dialog.add(buttonPanel, BorderLayout.SOUTH);

                    dialog.setSize(400, 500);
                    dialog.setLocationRelativeTo(this);
                    dialog.setVisible(true);
                }
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Error viewing group members: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Load the list of users.
     */
    private void loadUsers() {
        try {
            List<ChatUser> users = userService.getAllUsers();
            userListModel.clear();

            for (ChatUser user : users) {
                if (!user.getUsername().equals(currentUser.getUsername())) {
                    userListModel.addElement(user.getUsername());
                }
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
     * Load the list of groups.
     */
    private void loadGroups() {
        try {
            List<ChatGrp> groups = chatService.getUserGroups(currentUser.getUsername());
            groupListModel.clear();

            for (ChatGrp group : groups) {
                String groupName = group.getName();
                groupListModel.addElement(groupName);

                // Create a chat area for this group if it doesn't exist
                if (!chatAreas.containsKey(groupName)) {
                    // Create a new chat area for this group
                    JEditorPane groupChatArea = new JEditorPane("text/html", "");
                    groupChatArea.setEditable(false);
                    groupChatArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                    groupChatArea.setBackground(new Color(245, 245, 250)); // Same as backgroundColor
                    groupChatArea.setFont(new Font("Arial", Font.PLAIN, 12));

                    // Create a scroll pane for the chat area
                    JScrollPane scrollPane = new JScrollPane(groupChatArea);
                    scrollPane.setBorder(BorderFactory.createLineBorder(new Color(74, 101, 114), 1)); // primaryLightColor

                    // Add to the card panel
                    chatCardPanel.add(scrollPane, groupName);

                    // Store in the map
                    chatAreas.put(groupName, groupChatArea);

                    // Load messages for this group in the background
                    SwingUtilities.invokeLater(() -> {
                        try {
                            List<ChatMsg> messages = chatService.getGroupMessages(groupName);
                            displayMessages(messages, groupChatArea);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading groups: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    /**
     * Load messages for the selected user or group.
     */
    private void loadMessages() {
        try {
            if (isGroupSelected && selectedGroup != null) {
                if (selectedGroup.equals("All Groups")) {
                    // Handle the "All Groups" case
                    loadAllGroupMessages();
                    return;
                }

                // Check if we already have a chat area for this group
                if (!chatAreas.containsKey(selectedGroup)) {
                    // Create a new chat area for this group
                    JEditorPane groupChatArea = new JEditorPane("text/html", "");
                    groupChatArea.setEditable(false);
                    groupChatArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                    groupChatArea.setBackground(new Color(245, 245, 250)); // Same as backgroundColor
                    groupChatArea.setFont(new Font("Arial", Font.PLAIN, 12));

                    // Create a scroll pane for the chat area
                    JScrollPane scrollPane = new JScrollPane(groupChatArea);
                    scrollPane.setBorder(BorderFactory.createLineBorder(new Color(74, 101, 114), 1)); // primaryLightColor

                    // Add to the card panel
                    chatCardPanel.add(scrollPane, selectedGroup);

                    // Store in the map
                    chatAreas.put(selectedGroup, groupChatArea);
                }

                // Show the chat area for this group
                CardLayout cardLayout = (CardLayout) chatCardPanel.getLayout();
                cardLayout.show(chatCardPanel, selectedGroup);

                // Get the chat area for this group
                JEditorPane currentChatArea = chatAreas.get(selectedGroup);
                currentChatArea.setText("");

                // Load group messages
                List<ChatMsg> messages = chatService.getGroupMessages(selectedGroup);
                displayMessages(messages, currentChatArea);
            } else if (!isGroupSelected && selectedUser != null) {
                // For direct messages, use the default chat area
                chatArea.setText("");
                CardLayout cardLayout = (CardLayout) chatCardPanel.getLayout();
                cardLayout.show(chatCardPanel, "default");

                // Load direct messages
                List<ChatMsg> messages = chatService.getMessages(currentUser.getUsername(), selectedUser);
                displayMessages(messages, chatArea);
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading messages: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    /**
     * Load messages from all groups the user is a member of.
     */
    private void loadAllGroupMessages() {
        try {
            // Check if we already have a chat area for "All Groups"
            if (!chatAreas.containsKey("All Groups")) {
                // Create a new chat area for all groups
                allGroupsChatArea = new JEditorPane("text/html", "");
                allGroupsChatArea.setEditable(false);
                allGroupsChatArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                allGroupsChatArea.setBackground(new Color(245, 245, 250)); // Same as backgroundColor
                allGroupsChatArea.setFont(new Font("Arial", Font.PLAIN, 12));

                // Create a scroll pane for the chat area
                JScrollPane scrollPane = new JScrollPane(allGroupsChatArea);
                scrollPane.setBorder(BorderFactory.createLineBorder(new Color(74, 101, 114), 1)); // primaryLightColor

                // Add to the card panel
                chatCardPanel.add(scrollPane, "All Groups");

                // Store in the map
                chatAreas.put("All Groups", allGroupsChatArea);
            }

            // Show the chat area for all groups
            CardLayout cardLayout = (CardLayout) chatCardPanel.getLayout();
            cardLayout.show(chatCardPanel, "All Groups");

            // Get the chat area for all groups
            JEditorPane currentChatArea = chatAreas.get("All Groups");
            currentChatArea.setText("");

            // Get all groups the user is a member of
            List<ChatGrp> userGroups = chatService.getUserGroups(currentUser.getUsername());

            // Collect all messages from all groups
            List<ChatMsg> allMessages = new ArrayList<>();
            for (ChatGrp group : userGroups) {
                List<ChatMsg> groupMessages = chatService.getGroupMessages(group.getName());
                allMessages.addAll(groupMessages);
            }

            // Sort messages by sent time
            allMessages.sort((m1, m2) -> m1.getSentTime().compareTo(m2.getSentTime()));

            // Display all messages with group information
            displayMessages(allMessages, currentChatArea);
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading group messages: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    /**
     * Display messages in the specified chat area.
     *
     * @param messages The messages to display
     * @param targetChatArea The chat area to display messages in
     */
    private void displayMessages(List<ChatMsg> messages, JEditorPane targetChatArea) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; margin: 10px;'>");

        // Display the chat start time at the beginning
        if (!messages.isEmpty()) {
            try {
                if (isGroupSelected && selectedGroup != null) {
                    // For group chats, get the creation date from the group
                    List<ChatGrp> groups = chatService.getAllGroups();
                    ChatGrp selectedChatGrp = groups.stream()
                            .filter(g -> g.getName().equals(selectedGroup))
                            .findFirst()
                            .orElse(null);

                    if (selectedChatGrp != null) {
                        String startTime = DATE_FORMAT.format(selectedChatGrp.getCreatedDate());
                        sb.append("<div style='color: #4A6572; font-weight: bold; margin-bottom: 10px;'>");
                        sb.append("Chat started at : ").append(startTime);
                        sb.append("</div>");

                        // Display all members' joined and left time
                        sb.append("<div style='color: #4A6572; margin-bottom: 10px;'>");
                        sb.append("<h3>Group Members:</h3>");

                        try {
                            // Get all logs to find join and leave times
                            List<ChatLogs> allLogs = logService.getAllLogs();

                            // Create maps to store join and leave times for each user
                            Map<String, Date> joinTimes = new HashMap<>();
                            Map<String, Date> leaveTimes = new HashMap<>();
                            Date chatStopTime = null;

                            // Filter logs for this chat group and extract join/leave times
                            for (ChatLogs log : allLogs) {
                                if (log.getChatId() != null && log.getChatId().equals(selectedGroup)) {
                                    String username = log.getUser().getUsername();
                                    if (log.getAction().equals("JOIN")) {
                                        // Store the most recent join time
                                        if (!joinTimes.containsKey(username) ||
                                                log.getTimestamp().after(joinTimes.get(username))) {
                                            joinTimes.put(username, log.getTimestamp());
                                        }
                                    } else if (log.getAction().equals("LEAVE")) {
                                        // Store the most recent leave time
                                        if (!leaveTimes.containsKey(username) ||
                                                log.getTimestamp().after(leaveTimes.get(username))) {
                                            leaveTimes.put(username, log.getTimestamp());
                                        }
                                    } else if (log.getAction().equals("CHAT_STOPPED")) {
                                        // Store the most recent chat stop time
                                        if (chatStopTime == null || log.getTimestamp().after(chatStopTime)) {
                                            chatStopTime = log.getTimestamp();
                                        }
                                    }
                                }
                            }

                            for (ChatUser member : selectedChatGrp.getMembers()) {
                                String memberNickName = member.getNickName() != null ? member.getNickName() : member.getUsername();
                                String username = member.getUsername();
                                sb.append("<div style='margin-left: 15px; margin-bottom: 5px;'>");

                                // Use profile picture if available
                                sb.append(getProfilePicture(username, member.getProfilePicture()));

                                // If this member is the creator, show exact join time (group creation time)
                                if (username.equals(selectedChatGrp.getCreator().getUsername())) {
                                    sb.append(memberNickName).append(" - Joined: ").append(startTime).append(" (Creator)");
                                } else {
                                    // Show join time from logs if available
                                    String joinTime = joinTimes.containsKey(username) ?
                                            DATE_FORMAT.format(joinTimes.get(username)) : "Not available";
                                    sb.append(memberNickName).append(" - Joined: ").append(joinTime);
                                }

                                // Show leave time if available
                                if (leaveTimes.containsKey(username)) {
                                    sb.append(" - Left: ").append(DATE_FORMAT.format(leaveTimes.get(username)));
                                }

                                sb.append("</div>");
                            }

                            // Display chat stop time if available
                            if (chatStopTime != null) {
                                String stopTimeStr = DATE_FORMAT.format(chatStopTime);
                                sb.append("<div style='color: #4A6572; font-weight: bold; margin-top: 10px; text-align: center; padding: 10px; border-top: 1px solid #ccc;'>");
                                sb.append("Chat stopped at : ").append(stopTimeStr);
                                sb.append("</div>");
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            // Fallback to original behavior if there's an error
                            for (ChatUser member : selectedChatGrp.getMembers()) {
                                String memberNickName = member.getNickName() != null ? member.getNickName() : member.getUsername();
                                sb.append("<div style='margin-left: 15px; margin-bottom: 5px;'>");

                                // Use profile picture if available
                                sb.append(getProfilePicture(member.getUsername(), member.getProfilePicture()));

                                if (member.getUsername().equals(selectedChatGrp.getCreator().getUsername())) {
                                    sb.append(memberNickName).append(" - Joined: ").append(startTime).append(" (Creator)");
                                } else {
                                    sb.append(memberNickName).append(" - Join time not available");
                                }
                                sb.append("</div>");
                            }
                        }
                        sb.append("</div>");
                    }
                } else if (selectedUser != null) {
                    // For direct messages, use the timestamp of the first message
                    ChatMsg firstMessage = messages.get(0);
                    String startTime = DATE_FORMAT.format(firstMessage.getSentTime());
                    sb.append("<div style='color: #4A6572; font-weight: bold; margin-bottom: 10px;'>");
                    sb.append("Chat started at : ").append(startTime);
                    sb.append("</div>");

                    // Display user join and leave times for direct messages
                    sb.append("<div style='color: #4A6572; margin-bottom: 10px;'>");
                    sb.append("<h3>Chat Participants:</h3>");

                    try {
                        // Get all logs to find join and leave times
                        List<ChatLogs> allLogs = logService.getAllLogs();

                        // Create maps to store join and leave times for each user
                        Map<String, Date> joinTimes = new HashMap<>();
                        Map<String, Date> leaveTimes = new HashMap<>();
                        Date chatStopTime = null;

                        // Filter logs for this direct chat and extract join/leave times
                        for (ChatLogs log : allLogs) {
                            if (log.getChatId() != null &&
                                    (log.getChatId().equals(currentUser.getUsername() + "-" + selectedUser) ||
                                            log.getChatId().equals(selectedUser + "-" + currentUser.getUsername()))) {
                                String username = log.getUser().getUsername();
                                if (log.getAction().equals("JOIN")) {
                                    // Store the most recent join time
                                    if (!joinTimes.containsKey(username) ||
                                            log.getTimestamp().after(joinTimes.get(username))) {
                                        joinTimes.put(username, log.getTimestamp());
                                    }
                                } else if (log.getAction().equals("LEAVE")) {
                                    // Store the most recent leave time
                                    if (!leaveTimes.containsKey(username) ||
                                            log.getTimestamp().after(leaveTimes.get(username))) {
                                        leaveTimes.put(username, log.getTimestamp());
                                    }
                                } else if (log.getAction().equals("CHAT_STOPPED")) {
                                    // Store the most recent chat stop time
                                    if (chatStopTime == null || log.getTimestamp().after(chatStopTime)) {
                                        chatStopTime = log.getTimestamp();
                                    }
                                }
                            }
                        }

                        // Display current user's join/leave times
                        sb.append("<div style='margin-left: 15px; margin-bottom: 5px;'>");
                        sb.append(getProfilePicture(currentUser.getUsername(), currentUser.getProfilePicture()));
                        String currentUserNickName = currentUser.getNickName() != null ? currentUser.getNickName() : currentUser.getUsername();
                        String joinTime = joinTimes.containsKey(currentUser.getUsername()) ?
                                DATE_FORMAT.format(joinTimes.get(currentUser.getUsername())) : startTime;
                        sb.append(currentUserNickName).append(" - Joined: ").append(joinTime);
                        if (leaveTimes.containsKey(currentUser.getUsername())) {
                            sb.append(" - Left: ").append(DATE_FORMAT.format(leaveTimes.get(currentUser.getUsername())));
                        }
                        sb.append("</div>");

                        // Display selected user's join/leave times
                        ChatUser selectedUserObj = userService.getUserByUsername(selectedUser);
                        if (selectedUserObj != null) {
                            sb.append("<div style='margin-left: 15px; margin-bottom: 5px;'>");
                            sb.append(getProfilePicture(selectedUser, selectedUserObj.getProfilePicture()));
                            String selectedUserNickName = selectedUserObj.getNickName() != null ? selectedUserObj.getNickName() : selectedUser;
                            String selectedJoinTime = joinTimes.containsKey(selectedUser) ?
                                    DATE_FORMAT.format(joinTimes.get(selectedUser)) : startTime;
                            sb.append(selectedUserNickName).append(" - Joined: ").append(selectedJoinTime);
                            if (leaveTimes.containsKey(selectedUser)) {
                                sb.append(" - Left: ").append(DATE_FORMAT.format(leaveTimes.get(selectedUser)));
                            }
                            sb.append("</div>");
                        }

                        sb.append("</div>");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        for (ChatMsg message : messages) {
            ChatUser sender = message.getSender();
            String username = sender.getUsername();
            String nickName = sender.getNickName() != null ? sender.getNickName() : username;
            String timestamp = DATE_FORMAT.format(message.getSentTime());
            String content = message.getMessage();
            String profilePic = sender.getProfilePicture();

            // Get group information if this is the "All Groups" view
            String groupInfo = "";
            if (selectedGroup != null && selectedGroup.equals("All Groups") && message.getGroup() != null) {
                groupInfo = " <span style='color: #F9AA33;'>[" + message.getGroup().getName() + "]</span>";
            }

            // Format the message according to the required format
            if (content.equals("Hi")) {
                // For Hi greeting - display as a join notification
                sb.append("<div style='color: #4A6572; font-weight: bold; margin-top: 8px;'>");

                // Use profile picture if available
                sb.append(getProfilePicture(username, profilePic));

                sb.append(nickName).append(groupInfo).append(" has joined : ").append(timestamp);
                sb.append("</div>");
            } else if (content.equals("Bye")) {
                // For Bye message - display as a leave notification
                sb.append("<div style='color: #4A6572; font-weight: bold; margin-top: 8px;'>");

                // Use profile picture if available
                sb.append(getProfilePicture(username, profilePic));

                sb.append(nickName).append(groupInfo).append(" left : ").append(timestamp);
                sb.append("</div>");
            } else if (content.startsWith("@")) {
                // For mentions, highlight them
                sb.append("<div style='margin-top: 8px;'>");

                // Use profile picture if available
                sb.append(getProfilePicture(username, profilePic));

                sb.append("<span style='color: #344955; font-weight: bold;'>").append(nickName).append(groupInfo).append("</span><br>");
                sb.append("<span style='color: #F9AA33; margin-left: 15px;'>").append(content).append("</span>");
                sb.append("</div>");
            } else {
                // For regular messages
                sb.append("<div style='margin-top: 8px;'>");

                // Use profile picture if available
                sb.append(getProfilePicture(username, profilePic));

                sb.append("<span style='color: #344955; font-weight: bold;'>").append(nickName).append(groupInfo).append("</span><br>");
                sb.append("<span style='color: #232F34; margin-left: 15px;'>").append(content).append("</span>");
                sb.append("</div>");
            }
        }

        sb.append("</body></html>");
        targetChatArea.setText(sb.toString());
        // Scroll to bottom
        targetChatArea.setCaretPosition(0);
    }
    /**
     * Send a message to the selected user or group.
     */
    private void sendMessage() {
        String message = messageField.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        try {
            // Check if the message is "Bye" to leave the chat
            if (message.equalsIgnoreCase("Bye")) {
                if (isGroupSelected && selectedGroup != null) {
                    // Special handling for "All Groups" view
                    if (selectedGroup.equals("All Groups")) {
                        try {
                            List<ChatGrp> userGroups = chatService.getUserGroups(currentUser.getUsername());
                            if (userGroups.isEmpty()) {
                                JOptionPane.showMessageDialog(this,
                                        "You are not a member of any group.",
                                        "No Groups",
                                        JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }

                            List<String> groupNames = userGroups.stream()
                                    .map(ChatGrp::getName)
                                    .toList();

                            String chosenGroup = (String) JOptionPane.showInputDialog(
                                    this,
                                    "Select a group to leave:",
                                    "Select Group",
                                    JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    groupNames.toArray(),
                                    groupNames.get(0)
                            );

                            if (chosenGroup != null) {
                                // Leave the selected group
                                boolean success = chatService.removeUserFromGroup(chosenGroup, currentUser.getUsername());
                                if (success) {
                                    // Get the current date and time
                                    Date leaveTime = new Date();
                                    String nickName = currentUser.getNickName() != null ? currentUser.getNickName() : currentUser.getUsername();

                                    // Notify all users in the group that this user has left
                                    chatService.notifyUserLeft(chosenGroup, currentUser.getUsername(), nickName, leaveTime);

                                    messageField.setText("");
                                    JOptionPane.showMessageDialog(this,
                                            "You have left the group: " + chosenGroup,
                                            "Left Group",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    loadGroups();

                                    // Refresh the "All Groups" view
                                    loadAllGroupMessages();
                                } else {
                                    JOptionPane.showMessageDialog(this,
                                            "Failed to leave group: " + chosenGroup,
                                            "Error",
                                            JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        } catch (RemoteException ex) {
                            JOptionPane.showMessageDialog(this,
                                    "Error getting groups: " + ex.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                        }
                        return;
                    }

                    // Leave the group
                    boolean success = chatService.removeUserFromGroup(selectedGroup, currentUser.getUsername());
                    if (success) {
                        // Get the current date and time
                        Date leaveTime = new Date();
                        String nickName = currentUser.getNickName() != null ? currentUser.getNickName() : currentUser.getUsername();

                        // Display the leave message in the chat
                        String timeStr = DATE_FORMAT.format(leaveTime);

                        StringBuilder leaveMessageBuilder = new StringBuilder();
                        leaveMessageBuilder.append("<div style='color: #4A6572; font-weight: bold; margin-top: 8px;'>");

                        // Always use default profile icon instead of images
                        leaveMessageBuilder.append(getDefaultProfileIcon(nickName));

                        leaveMessageBuilder.append(nickName).append(" left : ").append(timeStr).append("</div>");
                        String leaveMessage = leaveMessageBuilder.toString();

                        // Get the chat area for this group
                        JEditorPane groupChatArea = chatAreas.get(selectedGroup);
                        if (groupChatArea != null) {
                            // Get current content without closing tags
                            String currentText = groupChatArea.getText();
                            currentText = currentText.replace("</body></html>", "");

                            // Add new message and close tags
                            groupChatArea.setText(currentText + leaveMessage + "</body></html>");
                        } else {
                            // Fallback to default chat area if group chat area doesn't exist
                            String currentText = chatArea.getText();
                            currentText = currentText.replace("</body></html>", "");

                            // Add new message and close tags
                            chatArea.setText(currentText + leaveMessage + "</body></html>");
                        }

                        // Notify all users in the group that this user has left
                        chatService.notifyUserLeft(selectedGroup, currentUser.getUsername(), nickName, leaveTime);

                        messageField.setText("");
                        JOptionPane.showMessageDialog(this,
                                "You have left the group: " + selectedGroup,
                                "Left Group",
                                JOptionPane.INFORMATION_MESSAGE);
                        loadGroups();

                        // Clear the chat area and reset selection
                        JEditorPane selectedGroupChatArea = chatAreas.get(selectedGroup);
                        if (selectedGroupChatArea != null) {
                            selectedGroupChatArea.setText("");
                        }
                        selectedGroup = null;
                        isGroupSelected = false;
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to leave group: " + selectedGroup,
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    return;
                } else if (!isGroupSelected && selectedUser != null) {
                    // Handle "Bye" for direct messages
                    // Get the current date and time
                    Date leaveTime = new Date();
                    String nickName = currentUser.getNickName() != null ? currentUser.getNickName() : currentUser.getUsername();

                    // Send the "Bye" message to notify the other user
                    ChatMsg chatMsg = chatService.sendMessage(currentUser.getUsername(), selectedUser, message);
                    if (chatMsg != null) {
                        // Display the leave message in the chat
                        String timeStr = DATE_FORMAT.format(leaveTime);

                        StringBuilder leaveMessageBuilder = new StringBuilder();
                        leaveMessageBuilder.append("<div style='color: #4A6572; font-weight: bold; margin-top: 8px;'>");

                        // Always use default profile icon instead of images
                        leaveMessageBuilder.append(getDefaultProfileIcon(nickName));

                        leaveMessageBuilder.append(nickName).append(" left : ").append(timeStr).append("</div>");
                        String leaveMessage = leaveMessageBuilder.toString();

                        // For direct messages, use the default chat area
                        String currentText = chatArea.getText();
                        currentText = currentText.replace("</body></html>", "");

                        // Add new message and close tags
                        chatArea.setText(currentText + leaveMessage + "</body></html>");

                        messageField.setText("");
                        JOptionPane.showMessageDialog(this,
                                "You have left the chat with: " + selectedUser,
                                "Left Chat",
                                JOptionPane.INFORMATION_MESSAGE);

                        // Store the selected user before clearing
                        String chatPartner = selectedUser;

                        // Clear the chat area and reset selection
                        chatArea.setText("");
                        selectedUser = null;

                        // Notify that the chat has stopped and exit the application
                        try {
                            // First notify that the user has left
                            chatService.notifyUserLeft(chatPartner, currentUser.getUsername(), nickName, leaveTime);
                            // Then notify that the chat has stopped
                            chatService.notifyChatStopped(chatPartner, leaveTime);
                        } catch (RemoteException ex) {
                            ex.printStackTrace();
                        }
                    }
                    return;
                }
            }

            if (isGroupSelected && selectedGroup != null) {
                if (selectedGroup.equals("All Groups")) {
                    // When "All Groups" is selected, send the message to all groups the user is a member of
                    try {
                        List<ChatGrp> userGroups = chatService.getUserGroups(currentUser.getUsername());
                        if (userGroups.isEmpty()) {
                            JOptionPane.showMessageDialog(this,
                                    "You are not a member of any group. Please join a group first.",
                                    "No Groups",
                                    JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        // Send message to all groups
                        boolean messageSent = false;
                        for (ChatGrp group : userGroups) {
                            ChatMsg chatMsg = chatService.sendGroupMessage(currentUser.getUsername(), group.getName(), message);
                            if (chatMsg != null) {
                                messageSent = true;
                            }
                        }

                        if (messageSent) {
                            messageField.setText("");
                            // Stay in the "All Groups" view but refresh the messages
                            loadAllGroupMessages();
                            JOptionPane.showMessageDialog(this,
                                    "Message sent to all groups",
                                    "Message Sent",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (RemoteException ex) {
                        JOptionPane.showMessageDialog(this,
                                "Error getting groups: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                } else {
                    // Send group message to the selected group
                    ChatMsg chatMsg = chatService.sendGroupMessage(currentUser.getUsername(), selectedGroup, message);
                    if (chatMsg != null) {
                        messageField.setText("");
                        loadMessages();
                    }
                }
            } else if (!isGroupSelected && selectedUser != null) {
                // Send direct message
                ChatMsg chatMsg = chatService.sendMessage(currentUser.getUsername(), selectedUser, message);
                if (chatMsg != null) {
                    messageField.setText("");
                    loadMessages();
                }
            } else if (isGroupSelected && selectedGroup == null) {
                // If a group chat is selected but no specific group is chosen,
                // show a dialog to select a group
                try {
                    List<ChatGrp> userGroups = chatService.getUserGroups(currentUser.getUsername());
                    if (userGroups.isEmpty()) {
                        JOptionPane.showMessageDialog(this,
                                "You are not a member of any group. Please join a group first.",
                                "No Groups",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    List<String> groupNames = userGroups.stream()
                            .map(ChatGrp::getName)
                            .toList();

                    String chosenGroup = (String) JOptionPane.showInputDialog(
                            this,
                            "Select a group to send message to:",
                            "Select Group",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            groupNames.toArray(),
                            groupNames.get(0)
                    );

                    if (chosenGroup != null) {
                        // Send message to the selected group
                        ChatMsg chatMsg = chatService.sendGroupMessage(currentUser.getUsername(), chosenGroup, message);
                        if (chatMsg != null) {
                            messageField.setText("");

                            // Set the selected group and load messages
                            selectedGroup = chosenGroup;
                            isGroupSelected = true;
                            selectedUser = null;
                            loadMessages();
                        }
                    }
                } catch (RemoteException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Error getting groups: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            } else if (!isGroupSelected && selectedUser == null) {
                // If no user is selected and we're not in a group chat,
                // show a dialog to select a user first
                JOptionPane.showMessageDialog(this,
                        "Please select a user from the list first before sending a message.",
                        "No User Selected",
                        JOptionPane.WARNING_MESSAGE);
                return;
            } else if (isGroupSelected && selectedGroup == null) {
                // We're in a group chat context but no group is selected
                // Show a dialog to select a group
                try {
                    List<ChatGrp> userGroups = chatService.getUserGroups(currentUser.getUsername());
                    if (userGroups.isEmpty()) {
                        JOptionPane.showMessageDialog(this,
                                "You are not a member of any group. Please join a group first.",
                                "No Groups",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    List<String> groupNames = userGroups.stream()
                            .map(ChatGrp::getName)
                            .toList();

                    String chosenGroup = (String) JOptionPane.showInputDialog(
                            this,
                            "Select a group to send message to:",
                            "Select Group",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            groupNames.toArray(),
                            groupNames.get(0)
                    );

                    if (chosenGroup != null) {
                        // Send message to the selected group
                        ChatMsg chatMsg = chatService.sendGroupMessage(currentUser.getUsername(), chosenGroup, message);
                        if (chatMsg != null) {
                            messageField.setText("");

                            // Set the selected group and load messages
                            selectedGroup = chosenGroup;
                            isGroupSelected = true;
                            selectedUser = null;
                            loadMessages();
                        }
                    }
                } catch (RemoteException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Error getting groups: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Error sending message: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Create a new group.
     */
    private void createGroup() {
        String groupName = JOptionPane.showInputDialog(this, "Enter group name:");
        if (groupName != null && !groupName.trim().isEmpty()) {
            String description = JOptionPane.showInputDialog(this, "Enter group description:");
            if (description != null) {
                try {
                    ChatGrp group = chatService.createGroup(groupName, description, currentUser.getUsername());
                    if (group != null) {
                        JOptionPane.showMessageDialog(this,
                                "Group created successfully",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);

                        // Create a chat area for this group
                        if (!chatAreas.containsKey(groupName)) {
                            // Create a new chat area for this group
                            JEditorPane groupChatArea = new JEditorPane("text/html", "");
                            groupChatArea.setEditable(false);
                            groupChatArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                            groupChatArea.setBackground(new Color(245, 245, 250)); // Same as backgroundColor
                            groupChatArea.setFont(new Font("Arial", Font.PLAIN, 12));

                            // Create a scroll pane for the chat area
                            JScrollPane scrollPane = new JScrollPane(groupChatArea);
                            scrollPane.setBorder(BorderFactory.createLineBorder(new Color(74, 101, 114), 1)); // primaryLightColor

                            // Add to the card panel
                            chatCardPanel.add(scrollPane, groupName);

                            // Store in the map
                            chatAreas.put(groupName, groupChatArea);

                            // Initialize with a welcome message
                            StringBuilder sb = new StringBuilder();
                            sb.append("<html><body style='font-family: Arial, sans-serif; margin: 10px;'>");
                            sb.append("<div style='color: #4A6572; font-weight: bold; margin-bottom: 10px;'>");
                            sb.append("Group created at : ").append(DATE_FORMAT.format(new Date()));
                            sb.append("</div></body></html>");
                            groupChatArea.setText(sb.toString());
                        }

                        loadGroups();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to create group. Group name may already be taken.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (RemoteException e) {
                    JOptionPane.showMessageDialog(this,
                            "Error creating group: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Join a group.
     */
    private void joinGroup() {
        try {
            // Get all available groups
            List<ChatGrp> allGroups = chatService.getAllGroups();

            // Filter out groups the user is already a member of
            List<ChatGrp> userGroups = chatService.getUserGroups(currentUser.getUsername());
            List<String> userGroupNames = userGroups.stream()
                    .map(ChatGrp::getName)
                    .toList();

            List<String> availableGroups = allGroups.stream()
                    .map(ChatGrp::getName)
                    .filter(name -> !userGroupNames.contains(name))
                    .toList();

            if (availableGroups.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No available groups to join.",
                        "Join Group",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Show dialog to select a group
            String selectedGroup = (String) JOptionPane.showInputDialog(
                    this,
                    "Select a group to join:",
                    "Join Group",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    availableGroups.toArray(),
                    availableGroups.get(0)
            );

            if (selectedGroup != null) {
                // Check if the user is subscribed to the group creator
                ChatGrp group = allGroups.stream()
                        .filter(g -> g.getName().equals(selectedGroup))
                        .findFirst()
                        .orElse(null);

                if (group != null) {
                    try {
                        // No subscription check needed - all users can join any group
                        // This allows for true group chat functionality where any user can join any group

                        boolean success = chatService.addUserToGroup(selectedGroup, currentUser.getUsername());
                        if (success) {
                            JOptionPane.showMessageDialog(this,
                                    "Successfully joined group: " + selectedGroup,
                                    "Join Group",
                                    JOptionPane.INFORMATION_MESSAGE);

                            // Notify all users in the group that this user has joined
                            chatService.notifyUserJoined(selectedGroup, currentUser.getUsername(),
                                    currentUser.getNickName() != null ? currentUser.getNickName() : currentUser.getUsername(),
                                    new Date());

                            // Create a chat area for this group if it doesn't exist
                            if (!chatAreas.containsKey(selectedGroup)) {
                                // Create a new chat area for this group
                                JEditorPane groupChatArea = new JEditorPane("text/html", "");
                                groupChatArea.setEditable(false);
                                groupChatArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                                groupChatArea.setBackground(new Color(245, 245, 250)); // Same as backgroundColor
                                groupChatArea.setFont(new Font("Arial", Font.PLAIN, 12));

                                // Create a scroll pane for the chat area
                                JScrollPane scrollPane = new JScrollPane(groupChatArea);
                                scrollPane.setBorder(BorderFactory.createLineBorder(new Color(74, 101, 114), 1)); // primaryLightColor

                                // Add to the card panel
                                chatCardPanel.add(scrollPane, selectedGroup);

                                // Store in the map
                                chatAreas.put(selectedGroup, groupChatArea);

                                // Load messages for this group
                                List<ChatMsg> messages = chatService.getGroupMessages(selectedGroup);
                                displayMessages(messages, groupChatArea);
                            }

                            loadGroups();
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "Failed to join group: " + selectedGroup,
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (RemoteException e) {
                        JOptionPane.showMessageDialog(this,
                                "Error joining group: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to find group: " + selectedGroup,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Error joining group: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Leave a group.
     */
    private void leaveGroup() {
        try {
            // Get groups the user is a member of
            List<ChatGrp> userGroups = chatService.getUserGroups(currentUser.getUsername());

            if (userGroups.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "You are not a member of any groups.",
                        "Leave Group",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            List<String> userGroupNames = userGroups.stream()
                    .map(ChatGrp::getName)
                    .toList();

            // Show dialog to select a group
            String selectedGroup = (String) JOptionPane.showInputDialog(
                    this,
                    "Select a group to leave:",
                    "Leave Group",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    userGroupNames.toArray(),
                    userGroupNames.get(0)
            );

            if (selectedGroup != null) {
                boolean success = chatService.removeUserFromGroup(selectedGroup, currentUser.getUsername());
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "Successfully left group: " + selectedGroup,
                            "Leave Group",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadGroups();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to leave group: " + selectedGroup,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Error leaving group: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }





