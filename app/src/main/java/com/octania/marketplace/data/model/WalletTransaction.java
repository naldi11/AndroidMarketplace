package com.octania.marketplace.data.model;

import com.google.gson.annotations.SerializedName;

public class WalletTransaction {
    private int id;
    private String type; // credit, debit
    private double amount;
    private String description;
    
    @SerializedName("created_at")
    private String createdAt;

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
