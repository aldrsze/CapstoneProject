package GUI; // Or a more general package like 'Data'

/**
 * A simple record to hold product data retrieved from the database.
 */
public record Product(
    int id,
    String name,
    String categoryName,
    double costPrice,
    double sellingPrice,
    int stock,
    double totalCost
) {}