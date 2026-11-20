package com.shopeasy.Foundation.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products",
        indexes = @Index(name = "idx_products_store", columnList = "store_id"))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private GroceryStore store;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQuantity;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Version
    private Long version;

    protected Product() {}

    public Product(GroceryStore store, BigDecimal price, String name, String description,
                   int stockQuantity, ProductCategory category) {
        this.store = store;
        this.price = price;
        this.name = name;
        this.description = description;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.status = ProductStatus.AVAILABLE;
    }

    public void removeFromStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > stockQuantity) {
            throw new IllegalStateException("Insufficient stock for " + name);
        }

        stockQuantity -= quantity;
        if (stockQuantity == 0) {
            status = ProductStatus.OUT_OF_STOCK;
        }
    }

    public void addToStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        stockQuantity = Math.addExact(stockQuantity, quantity);
        if (status == ProductStatus.OUT_OF_STOCK) {
            status = ProductStatus.AVAILABLE;
        }
    }

    public Long getProductId() {
        return productId;
    }

    public GroceryStore getStore() {
        return store;
    }

    public BigDecimal getPrice() {
        return price;
    }
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }
    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public ProductCategory getCategory() {
        return category;
    }
    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public ProductStatus getStatus() {
        return status;
    }
    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public boolean isAvailable() {
        return status == ProductStatus.AVAILABLE && stockQuantity > 0;
    }
}
