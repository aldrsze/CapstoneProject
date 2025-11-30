package com.inventorysystem.model;

// Holds data for the stock summary report.
public record StockRecord(
    int productId,
    String productName,
    String categoryName,
    int stockIn,      // From logs
    int stockOut,     // From sales
    int endingStock   // Current stock
) {}