package com.inventorysystem.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

// Simple logger to help debug issues - writes to console and file
public class DebugLogger {
    
    private static final boolean ENABLE_LOGGING = true;
    private static final String LOG_FILE = "smartstock_debug.log";
    
    // Log info message (normal operations)
    public static void info(String message) {
        if (ENABLE_LOGGING) {
            String formatted = formatMessage("INFO", message);
            System.out.println(formatted);
            writeToFile(formatted);
        }
    }
    
    // Log error message (something went wrong)
    public static void error(String message) {
        String formatted = formatMessage("ERROR", message);
        System.err.println(formatted);
        writeToFile(formatted);
    }
    
    // Log error with exception details
    public static void error(String message, Exception e) {
        String formatted = formatMessage("ERROR", message + " - " + e.getMessage());
        System.err.println(formatted);
        e.printStackTrace();
        writeToFile(formatted);
        writeToFile(getStackTrace(e));
    }
    
    // Log debug message (detailed info for troubleshooting)
    public static void debug(String message) {
        if (ENABLE_LOGGING) {
            String formatted = formatMessage("DEBUG", message);
            System.out.println(formatted);
            writeToFile(formatted);
        }
    }
    
    // Format message with timestamp
    private static String formatMessage(String level, String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s] [%s] %s", sdf.format(new Date()), level, message);
    }
    
    // Write to log file
    private static void writeToFile(String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(message);
        } catch (IOException e) {
            // Can't log the logging error, just print it
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
    
    // Get full stack trace as string
    private static String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
