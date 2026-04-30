package com.octania.marketplace.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Room Entity for User - used for offline caching
 */
@Entity(tableName = "users")
public class UserEntity {
    
    @PrimaryKey
    @NonNull
    private int id;
    
    private String name;
    private String email;
    private String phone;
    private String avatar;
    private boolean isSeller;
    
    // Getters and Setters
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public boolean isSeller() { return isSeller; }
    public void setSeller(boolean seller) { isSeller = seller; }
}
