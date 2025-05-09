package com.example.chatapp.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.example.chatapp.domain.ChatMsg;

/**
 * Utility class for writing chat logs to text files
 */
public class FileWriter {

    private static final String BASE_DIR = "logs";

    /**
     * Saves chat messages to a text file
     *
     * @param chatName The name of the chat
     * @param chatId The unique ID of the chat
     * @param messages The list of chat messages
     * @param stopTime The time when the chat was stopped
     * @return The path to the saved file
     * @throws IOException If an error occurs while writing to the file
     */
    public static String saveChatToFile(String chatName, String chatId, List<ChatMsg> messages, Date stopTime) throws IOException {
        // Create directory if it doesn't exist
        File dir = new File(BASE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Create file path
        String fileName = chatName + "_" + chatId + ".txt";
        String filePath = BASE_DIR + File.separator + fileName;
        File file = new File(filePath);

        // Write messages to file
        try (PrintWriter writer = new PrintWriter(new java.io.FileWriter(file))) {
            // Write chat header
            writer.println("Chat: " + chatName);
            writer.println("Chat ID: " + chatId);
            writer.println("----------------------------------------");

            // Write all messages
            for (ChatMsg msg : messages) {
                String sender = msg.getSender().getNickName() != null ?
                        msg.getSender().getNickName() : msg.getSender().getUsername();
                writer.println(sender + ": " + msg.getMessage() + " : " +
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(msg.getSentTime()));
            }

            // Write chat stop time
            writer.println("----------------------------------------");
            writer.println("Chat stopped at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(stopTime));
        }

        return filePath;
    }
}