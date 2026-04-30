package com.octania.marketplace.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.octania.marketplace.data.local.entity.UserEntity;

/**
 * Room DAO for User entity
 */
@Dao
public interface UserDao {
    
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    UserEntity getUserById(int userId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);
    
    @Update
    void updateUser(UserEntity user);
    
    @Query("DELETE FROM users WHERE id = :userId")
    void deleteUser(int userId);
    
    @Query("DELETE FROM users")
    void deleteAllUsers();
}
