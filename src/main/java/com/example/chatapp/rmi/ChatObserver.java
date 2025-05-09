package com.example.chatapp.rmi;

import com.example.chatapp.domain.ChatMsg;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

/**
 * Remote interface for the Observer pattern.
 * Clients implement this interface to receive notifications about new messages.
 */
public interface ChatObserver extends Remote {

    /**
     * Called when a new message is received.
     *
     * @param message The new message
     * @throws RemoteException If a remote communication error occurs
     */
    void update(ChatMsg message) throws RemoteException;

    /**
     * Called when a user's online status changes.
     *
     * @param username The username of the user whose status changed
     * @param online True if the user is now online, false otherwise
     * @throws RemoteException If a remote communication error occurs
     */
    void userStatusChanged(String username, boolean online) throws RemoteException;

    /**
     * Called when a chat is started by an admin.
     *
     * @param chatName The name of the chat
     * @param startTime The time when the chat was started
     * @throws RemoteException If a remote communication error occurs
     */
    void chatStarted(String chatName, Date startTime) throws RemoteException;

    /**
     * Called when a user joins a chat.
     *
     * @param chatName The name of the chat
     * @param username The username of the user who joined
     * @param nickName The nickname of the user who joined
     * @param joinTime The time when the user joined
     * @throws RemoteException If a remote communication error occurs
     */
    void userJoined(String chatName, String username, String nickName, Date joinTime) throws RemoteException;

    /**
     * Called when a user leaves a chat.
     *
     * @param chatName The name of the chat
     * @param username The username of the user who left
     * @param nickName The nickname of the user who left
     * @param leaveTime The time when the user left
     * @throws RemoteException If a remote communication error occurs
     */
    void userLeft(String chatName, String username, String nickName, Date leaveTime) throws RemoteException;

    /**
     * Called when a chat is stopped (after the last user leaves).
     *
     * @param chatName The name of the chat
     * @param stopTime The time when the chat was stopped
     * @throws RemoteException If a remote communication error occurs
     */
    void chatStopped(String chatName, Date stopTime) throws RemoteException;

    /**
     * Get the username of this observer.
     *
     * @return The username
     * @throws RemoteException If a remote communication error occurs
     */
    String getUsername() throws RemoteException;
}
