package com.octania.marketplace.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Transaction {
    private int id;

    @SerializedName("buyer_id")
    private int buyerId;

    @SerializedName("total_amount")
    private double totalAmount;

    private String status;

    @SerializedName("payment_method_code")
    private String paymentMethodCode;

    @SerializedName("shipping_address_id")
    private Integer shippingAddressId;

    @SerializedName("payment_proof")
    private String paymentProof;

    @SerializedName("created_at")
    private String createdAt;

    private List<TransactionDetail> items;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(int buyerId) {
        this.buyerId = buyerId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethodCode() {
        return paymentMethodCode;
    }

    public void setPaymentMethodCode(String paymentMethodCode) {
        this.paymentMethodCode = paymentMethodCode;
    }

    public Integer getShippingAddressId() {
        return shippingAddressId;
    }

    public void setShippingAddressId(Integer shippingAddressId) {
        this.shippingAddressId = shippingAddressId;
    }

    public String getPaymentProof() {
        return paymentProof;
    }

    public void setPaymentProof(String paymentProof) {
        this.paymentProof = paymentProof;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<TransactionDetail> getItems() {
        return items;
    }

    public void setItems(List<TransactionDetail> items) {
        this.items = items;
    }
}
