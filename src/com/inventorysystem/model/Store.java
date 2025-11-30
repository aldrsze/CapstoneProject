package com.inventorysystem.model;

// Holds store info (name, location, contact)
public class Store {
    private final int storeId;
    private final int userId;
    private final String name;
    private final String location;
    private final String contact;

    public Store(int storeId, int userId, String name, String location, String contact) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Store name cannot be empty");
        }
        this.storeId = storeId;
        this.userId = userId;
        this.name = name;
        this.location = (location == null) ? "" : location;
        this.contact = (contact == null) ? "" : contact;
    }

    // Getters
    public int storeId() { return storeId; }
    public int userId() { return userId; }
    public String name() { return name; }
    public String location() { return location; }
    public String contact() { return contact; }

    @Override
    public String toString() {
        return "Store{id=" + storeId + ", user=" + userId + ", name='" + name + "'}";
    }
}