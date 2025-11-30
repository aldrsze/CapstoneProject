package com.inventorysystem.model;

import java.sql.Timestamp;

// Holds data for a single transaction row (sale or stock change).
public record TransactionRecord(
    Timestamp transactionDate,
    String productName,
    String transactionType,
    int quantity,
    double unitPrice,
    double retailPrice,
    double total
) {}