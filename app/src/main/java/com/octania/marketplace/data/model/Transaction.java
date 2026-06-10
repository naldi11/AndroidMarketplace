package com.octania.marketplace.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Transaction {
    private int id;

    @SerializedName("transaction_number")
    private String transactionNumber;

    @SerializedName("buyer_id")
    private int buyerId;

    @SerializedName("total_amount")
    private double totalAmount;

    @SerializedName("seller_amount")
    private double sellerAmount;

    private String status;

    @SerializedName("payment_method")
    private String paymentMethod;

    @SerializedName("payment_method_code")
    private String paymentMethodCode;


    @SerializedName("expires_at")
    private String expiresAt;

    @SerializedName("shipping_address_id")
    private Integer shippingAddressId;

    @SerializedName("payment_proof")
    private String paymentProof;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("paid_at")
    private String paidAt;

    private List<TransactionDetail> items;

    // ===== Getters & Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTransactionNumber() { return transactionNumber; }
    public void setTransactionNumber(String transactionNumber) { this.transactionNumber = transactionNumber; }

    public int getBuyerId() { return buyerId; }
    public void setBuyerId(int buyerId) { this.buyerId = buyerId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getSellerAmount() { return sellerAmount; }
    public void setSellerAmount(double sellerAmount) { this.sellerAmount = sellerAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentMethodCode() { return paymentMethodCode; }
    public void setPaymentMethodCode(String paymentMethodCode) { this.paymentMethodCode = paymentMethodCode; }


    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public Integer getShippingAddressId() { return shippingAddressId; }
    public void setShippingAddressId(Integer shippingAddressId) { this.shippingAddressId = shippingAddressId; }

    public String getPaymentProof() { return paymentProof; }
    public void setPaymentProof(String paymentProof) { this.paymentProof = paymentProof; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getPaidAt() { return paidAt; }
    public void setPaidAt(String paidAt) { this.paidAt = paidAt; }

    public List<TransactionDetail> getItems() { return items; }
    public void setItems(List<TransactionDetail> items) { this.items = items; }
}
