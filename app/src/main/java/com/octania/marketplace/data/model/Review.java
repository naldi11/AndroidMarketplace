package com.octania.marketplace.data.model;

import com.google.gson.annotations.SerializedName;

public class Review {
    private int id;
    private int rating;
    private String comment;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("reviewer")
    private User reviewer;

    public int getId() {
        return id;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public User getReviewer() {
        return reviewer;
    }
}
