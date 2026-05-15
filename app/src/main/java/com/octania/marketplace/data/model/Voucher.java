package com.octania.marketplace.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Voucher implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("user_voucher_id")
    private int userVoucherId;
    
    @SerializedName("code")
    private String code;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("discount_type")
    private String discountType; // fixed or percent
    
    @SerializedName("discount_amount")
    private double discountAmount;
    
    @SerializedName("max_discount_amount")
    private double maxDiscountAmount;
    
    @SerializedName("min_purchase")
    private double minPurchase;
    
    @SerializedName("terms")
    private String terms;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("is_valid")
    private boolean isValid;
    
    @SerializedName("invalid_reason")
    private String invalidReason;

    @SerializedName("is_claimed")
    private boolean isClaimed;

    // Getters and Setters
    public int getUserVoucherId() { return userVoucherId; }
    public void setUserVoucherId(int userVoucherId) { this.userVoucherId = userVoucherId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name != null ? name : code; }
    public void setName(String name) { this.name = name; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(double maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public double getMinPurchase() { return minPurchase; }
    public void setMinPurchase(double minPurchase) { this.minPurchase = minPurchase; }

    public String getTerms() { return terms; }
    public void setTerms(String terms) { this.terms = terms; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }

    public String getInvalidReason() { return invalidReason; }
    public void setInvalidReason(String invalidReason) { this.invalidReason = invalidReason; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public boolean isClaimed() { return isClaimed; }
    public void setClaimed(boolean claimed) { isClaimed = claimed; }
}
