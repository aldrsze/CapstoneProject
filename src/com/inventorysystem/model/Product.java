package com.inventorysystem.model;

// Product data model
public record Product(
    int id,
    String name,
    String categoryName,
    double costPrice,
    double retailPrice,
    int stock,
    double totalCost
) {}