package com.example.chatapp.rmi;

import com.example.chatapp.domain.ChatGrp;
import com.example.chatapp.domain.ChatMsg;
import com.example.chatapp.domain.ChatUser;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

/**
 * Remote interface for chat services.
 * Provides methods for sending messages, managing groups, and handling observers.
 */
public interface ChatService extends Remote {

    /**
     * Send a message to a specific user.
     *
     * @param sender The sender's username
     * @param receiver The receiver's username
     * @param message The message content
     * @return The created ChatMsg object
     * @throws RemoteException If a remote communication error occurs
     */
    ChatMsg sendMessage(String sender, String receiver, String message) throws RemoteException;

    /**
     * Send a message to a group.
     *
     * @param sender The sender's username
     * @param groupName The group name
     * @param message The message content
     * @return The created ChatMsg object
     * @throws RemoteException If a remote communication error occurs
     */
    ChatMsg sendGroupMessage(String sender, String groupName, String message) throws RemoteException;

    /**
     * Get all messages between two users.
     *
     * @param user1 The first user's username
     * @param user2 The second user's username
     * @return List of messages between the two users
     * @throws RemoteException If a remote communication error occurs
     */
    List<ChatMsg> getMessages(String user1, String user2) throws RemoteException;

    /**
     * Get all messages in a group.
     *
     * @param groupName The group name
     * @return List of messages in the group
     * @throws RemoteException If a remote communication error occurs
     */
    List<ChatMsg> getGroupMessages(String groupName) throws RemoteException;

    /**
     * Create a new group.
     *
     * @param groupName The group name
     * @param description The group description
     * @param creatorUsername The username of the group creator
     * @return The created ChatGrp object
     * @throws RemoteException If a remote communication error occurs
     */
    ChatGrp createGroup(String groupName, String description, String creatorUsername) throws RemoteException;

    /**
     * Add a user to a group.
     *
     * @param groupName The group name
     * @param username The username to add
     * @return True if the user was added successfully, false otherwise
     * @throws RemoteException If a remote communication error occurs
     */
    boolean addUserToGroup(String groupName, String username) throws RemoteException;

    /**
     * Remove a user from a group.
     *
     * @param groupName The group name
     * @param username The username to remove
     * @return True if the user was removed successfully, false otherwise
     * @throws RemoteException If a remote communication error occurs
     */
    boolean removeUserFromGroup(String groupName, String username) throws RemoteException;

    /**
     * Get all groups.
     *
     * @return List of all groups
     * @throws RemoteException If a remote communication error occurs
     */
    List<ChatGrp> getAllGroups() throws RemoteException;

    /**
     * Get all groups that a user is a member of.
     *
     * @param username The username
     * @return List of groups the user is a member of
     * @throws RemoteException If a remote communication error occurs
     */
    List<ChatGrp> getUserGroups(String username) throws RemoteException;

    /**
     * Register an observer to receive notifications.
     *
     * @param observer The observer to register
     * @throws RemoteException If a remote communication error occurs
     */
    void registerObserver(ChatObserver observer) throws RemoteException;

    /**
     * Unregister an observer to stop receiving notifications.
     *
     * @param observer The observer to unregister
     * @throws RemoteException If a remote communication error occurs
     */
    void unregisterObserver(ChatObserver observer) throws RemoteException;

    /**
     * Get all online users.
     *
     * @return List of online users
     * @throws RemoteException If a remote communication error occurs
     */
    List<ChatUser> getOnlineUsers() throws RemoteException;

    /**
     * Notify all subscribed and online users that a chat has started.
     *
     * @param chatName The name of the chat
     * @param startTime The time when the chat was started
     * @throws RemoteException If a remote communication error occurs
     */
    void notifyChatStarted(String chatName, Date startTime) throws RemoteException;

    /**
     * Notify all users in a chat that a user has joined.
     *
     * @param chatName The name of the chat
     * @param username The username of the user who joined
     * @param nickName The nickname of the user who joined
     * @param joinTime The time when the user joined
     * @throws RemoteException If a remote communication error occurs
     */
    void notifyUserJoined(String chatName, String username, String nickName, Date joinTime) throws RemoteException;

    /**
     * Notify all users in a chat that a user has left.
     *
     * @param chatName The name of the chat
     * @param username The username of the user who left
     * @param nickName The nickname of the user who left
     * @param leaveTime The time when the user left
     * @throws RemoteException If a remote communication error occurs
     */
    void notifyUserLeft(String chatName, String username, String nickName, Date leaveTime) throws RemoteException;

    /**
     * Notify all users that a chat has stopped (after the last user leaves).
     *
     * @param chatName The name of the chat
     * @param stopTime The time when the chat was stopped
     * @throws RemoteException If a remote communication error occurs
     */
    void notifyChatStopped(String chatName, Date stopTime) throws RemoteException;

    /**
     * Save chat logs to a file.
     *
     * @param chatName The name of the chat
     * @param chatId The unique identifier for the chat
     * @return The path to the saved file
     * @throws RemoteException If a remote communication error occurs
     */
    String saveChatLogs(String chatName, String chatId) throws RemoteException;
}
