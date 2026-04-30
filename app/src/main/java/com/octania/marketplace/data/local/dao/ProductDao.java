package com.octania.marketplace.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import com.octania.marketplace.data.local.entity.ProductEntity;

import java.util.Date;
import java.util.List;

/**
 * Room DAO for Product entity
 */
@Dao
public interface ProductDao {
    
    @Query("SELECT * FROM products ORDER BY distanceKm ASC")
    List<ProductEntity> getAllProducts();
    
    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    ProductEntity getProductById(int productId);
    
    @Query("SELECT * FROM products WHERE categoryName = :category ORDER BY distanceKm ASC")
    List<ProductEntity> getProductsByCategory(String category);
    
    @Query("SELECT * FROM products WHERE name LIKE '%' || :searchQuery || '%' ORDER BY distanceKm ASC")
    List<ProductEntity> searchProducts(String searchQuery);
    
    @Query("SELECT * FROM products WHERE isWishlisted = 1")
    List<ProductEntity> getWishlistedProducts();
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProduct(ProductEntity product);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllProducts(List<ProductEntity> products);
    
    @Query("DELETE FROM products WHERE id = :productId")
    void deleteProduct(int productId);
    
    @Query("DELETE FROM products")
    void deleteAllProducts();
    
    @Query("DELETE FROM products WHERE cachedAt < :expirationDate")
    void deleteExpiredCache(Date expirationDate);
    
    @Query("SELECT COUNT(*) FROM products")
    int getProductCount();
    
    @Query("UPDATE products SET isWishlisted = :wishlisted WHERE id = :productId")
    void updateWishlistStatus(int productId, boolean wishlisted);
    
    @Query("SELECT * FROM products WHERE distanceKm <= :maxDistance ORDER BY distanceKm ASC")
    List<ProductEntity> getProductsWithinDistance(double maxDistance);
}
