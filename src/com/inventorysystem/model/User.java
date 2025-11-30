package com.inventorysystem.model;

// Holds logged-in user data (ID, username, role).
public record User(int id, String username, String role) {
}