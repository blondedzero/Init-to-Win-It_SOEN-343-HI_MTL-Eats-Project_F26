package com.shopeasy.Domain.GS;

import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Foundation.entities.GroceryStore;
import com.shopeasy.Foundation.entities.Product;
import com.shopeasy.Foundation.entities.ProductCategory;
import com.shopeasy.Foundation.entities.ProductStatus;
import com.shopeasy.Foundation.entities.User;
import com.shopeasy.Foundation.repositories.ProductCategoryRepository;
import com.shopeasy.Foundation.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

// R3 (Extract Class): a store representative's catalog (5.5.2-6) - add, edit,
// remove and restore products, and list categories. Extracted from GSModule so
// that store status, catalog and order views each have one reason to change.
// Anything about products (promotions, reviews, stock rules) goes here.
@Service
public class CatalogModule {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final StoreAccess storeAccess;
    private final EventManager eventManager;

    public CatalogModule(ProductRepository productRepository, ProductCategoryRepository productCategoryRepository,
                         StoreAccess storeAccess, EventManager eventManager) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.storeAccess = storeAccess;
        this.eventManager = eventManager;
    }

    @Transactional(readOnly = true)
    public List<ProductData> getMyProducts(User user) {
        GroceryStore store = storeAccess.requireOwnStore(user, "view a catalog");
        return productRepository.findByStore_AccountId(store.getAccountId()).stream()
                .sorted(Comparator.comparing(Product::getName))
                .map(ProductData::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return productCategoryRepository.findAll().stream()
                .map(ProductCategory::getCategoryName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Transactional
    public ProductData addProduct(User user, ProductRequest req) {
        GroceryStore store = storeAccess.requireOwnStore(user, "add products");
        validateProductRequest(req);

        Product product = new Product(store, req.price(), req.name().trim(), req.description(),
                req.stockQuantity(), findOrCreateCategory(req.categoryName()));
        if (product.getStockQuantity() == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        Product saved = productRepository.save(product);
        eventManager.logEvent("Store " + store.getAccountId() + ": added product " + saved.getProductId()
                + " (" + saved.getName() + ")");
        return ProductData.from(saved);
    }

    @Transactional
    public ProductData updateProduct(User user, Long productId, ProductRequest req) {
        GroceryStore store = storeAccess.requireOwnStore(user, "edit products");
        Product product = requireOwnProduct(store, productId);
        validateProductRequest(req);

        product.setName(req.name().trim());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setStockQuantity(req.stockQuantity());
        product.setCategory(findOrCreateCategory(req.categoryName()));

        if (product.getStatus() != ProductStatus.UNAVAILABLE) {
            product.setStatus(product.getStockQuantity() > 0
                    ? ProductStatus.AVAILABLE
                    : ProductStatus.OUT_OF_STOCK);
        }

        eventManager.logEvent("Store " + store.getAccountId() + ": edited product " + productId);
        return ProductData.from(product);
    }

    @Transactional
    public ProductData removeProduct(User user, Long productId) {
        GroceryStore store = storeAccess.requireOwnStore(user, "remove products");
        Product product = requireOwnProduct(store, productId);

        product.setStatus(ProductStatus.UNAVAILABLE);

        eventManager.logEvent("Store " + store.getAccountId() + ": removed product " + productId);
        return ProductData.from(product);
    }

    @Transactional
    public ProductData restoreProduct(User user, Long productId) {
        GroceryStore store = storeAccess.requireOwnStore(user, "restore products");
        Product product = requireOwnProduct(store, productId);

        product.setStatus(product.getStockQuantity() > 0
                ? ProductStatus.AVAILABLE
                : ProductStatus.OUT_OF_STOCK);

        eventManager.logEvent("Store " + store.getAccountId() + ": restored product " + productId);
        return ProductData.from(product);
    }

    private Product requireOwnProduct(GroceryStore store, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " does not exist"));
        if (!product.getStore().getAccountId().equals(store.getAccountId())) {
            throw new PermissionDeniedException("Stores can only modify their own products");
        }
        return product;
    }

    private ProductCategory findOrCreateCategory(String categoryName) {
        String name = categoryName.trim();
        return productCategoryRepository.findByCategoryName(name)
                .orElseGet(() -> productCategoryRepository.save(new ProductCategory(name)));
    }

    private static void validateProductRequest(ProductRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("A product is required");
        }
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (req.price() == null || req.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be zero or more");
        }
        if (req.stockQuantity() == null || req.stockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity must be zero or more");
        }
        if (req.categoryName() == null || req.categoryName().isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }
    }
}
