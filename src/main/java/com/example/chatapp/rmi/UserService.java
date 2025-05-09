package com.example.chatapp.rmi;

import com.example.chatapp.domain.ChatUser;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface for user management services.
 * Provides methods for user registration, authentication, and profile management.
 */
public interface UserService extends Remote {

    /**
     * Register a new user.
     *
     * @param username The username
     * @param password The password
     * @param email The email address
     * @param nickName The nick name
     * @param profilePicture The profile picture path or URL
     * @return The created ChatUser object, or null if registration failed
     * @throws RemoteException If a remote communication error occurs
     */
    ChatUser register(String username, String password, String email, String nickName, String profilePicture) throws RemoteException;

    /**
     * Authenticate a user.
     *
     * @param username The username
     * @param password The password
     * @return The authenticated ChatUser object, or null if authentication failed
     * @throws RemoteException If a remote communication error occurs
     */
    ChatUser login(String username, String password) throws RemoteException;

    /**
     * Log out a user.
     *
     * @param username The username
     * @return True if logout was successful, false otherwise
     * @throws RemoteException If a remote communication error occurs
     */
    boolean logout(String username) throws RemoteException;

    /**
     * Get a user by username.
     *
     * @param username The username
     * @return The ChatUser object, or null if not found
     * @throws RemoteException If a remote communication error occurs
     */
    ChatUser getUserByUsername(String username) throws RemoteException;

    /**
     * Update a user's profile.
     *
     * @param username The username
     * @param newPassword The new password (null if not changing)
     * @param newEmail The new email (null if not changing)
     * @param newNickName The new nick name (null if not changing)
     * @param newProfilePicture The new profile picture path or URL (null if not changing)
     * @return The updated ChatUser object, or null if update failed
     * @throws RemoteException If a remote communication error occurs
     */
    ChatUser updateProfile(String username, String newPassword, String newEmail, String newNickName, String newProfilePicture) throws RemoteException;

    /**
     * Get all users.
     *
     * @return List of all users
     * @throws RemoteException If a remote communication error occurs
     */
    List<ChatUser> getAllUsers() throws RemoteException;

    /**
     * Check if a username is available.
     *
     * @param username The username to check
     * @return True if the username is available, false otherwise
     * @throws RemoteException If a remote communication error occurs
     */
    boolean isUsernameAvailable(String username) throws RemoteException;

    /**
     * Create an admin user (only if no admin exists).
     *
     * @param username The username
     * @param password The password
     * @param email The email address
     * @return The created admin ChatUser object, or null if creation failed
     * @throws RemoteException If a remote communication error occurs
     */
    ChatUser createAdmin(String username, String password, String email) throws RemoteException;

    /**
     * Check if a user is an admin.
     *
     * @param username The username
     * @return True if the user is an admin, false otherwise
     * @throws RemoteException If a remote communication error occurs
     */
    boolean isAdmin(String username) throws RemoteException;

    /**
     * Remove a user from the application.
     * Only admin users can perform this operation.
     *
     * @param adminUsername The username of the admin performing the operation
     * @param userToRemove The username of the user to remove
     * @return True if the user was removed successfully, false otherwise
     * @throws RemoteException If a remote communication error occurs
     */
    boolean removeUser(String adminUsername, String userToRemove) throws RemoteException;
}

