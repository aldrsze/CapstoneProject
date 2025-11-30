package com.inventorysystem.data;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Handles connecting to MySQL database
public class DatabaseConnection {

    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    // Load database settings from config.properties file
    static {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConnection.class.getResourceAsStream("/config.properties")) {
            if (input == null) {
                throw new RuntimeException("Cannot find config.properties file");
            }
            properties.load(input);
            DB_URL = properties.getProperty("db.url");
            DB_USER = properties.getProperty("db.user");
            DB_PASSWORD = properties.getProperty("db.password");

            if (DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
                throw new RuntimeException("Database config is incomplete in config.properties");
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load database configuration", ex);
        }
    }

    private DatabaseConnection() {}

    // Get a connection to the database
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}