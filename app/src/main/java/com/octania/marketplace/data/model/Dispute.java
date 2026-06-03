package com.octania.marketplace.data.model;

import com.google.gson.annotations.SerializedName;

public class Dispute {
    @SerializedName("id")                          private int id;
    @SerializedName("transaction_id")              private int transactionId;
    @SerializedName("buyer_id")                    private int buyerId;
    @SerializedName("seller_id")                   private int sellerId;
    @SerializedName("reason")                      private String reason;
    @SerializedName("description")                 private String description;
    @SerializedName("status")                      private String status;
    @SerializedName("winner")                      private String winner;
    @SerializedName("admin_notes")                 private String adminNotes;
    @SerializedName("return_courier")              private String returnCourier;
    @SerializedName("return_tracking_number")      private String returnTrackingNumber;
    @SerializedName("buyer_shipped_back_at")       private String buyerShippedBackAt;
    @SerializedName("seller_received_back_at")     private String sellerReceivedBackAt;
    @SerializedName("created_at")                  private String createdAt;

    public int getId()                      { return id; }
    public int getTransactionId()           { return transactionId; }
    public int getBuyerId()                 { return buyerId; }
    public int getSellerId()                { return sellerId; }
    public String getReason()               { return reason; }
    public String getDescription()          { return description; }
    public String getStatus()               { return status; }
    public String getWinner()               { return winner; }
    public String getAdminNotes()           { return adminNotes; }
    public String getReturnCourier()        { return returnCourier; }
    public String getReturnTrackingNumber() { return returnTrackingNumber; }
    public String getBuyerShippedBackAt()   { return buyerShippedBackAt; }
    public String getSellerReceivedBackAt() { return sellerReceivedBackAt; }
    public String getCreatedAt()            { return createdAt; }
}
