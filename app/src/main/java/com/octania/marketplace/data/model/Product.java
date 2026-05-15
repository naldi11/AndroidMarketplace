package com.octania.marketplace.data.model;

import com.google.gson.annotations.SerializedName;

public class Product {
    private int id;
    private String name;
    private String slug;
    private String description;
    private double price;
    private int stock;

    private String image;

    private Double latitude;
    private Double longitude;

    private String condition;
    private Integer weight;
    private String location;

    @SerializedName("effective_price")
    private double effectivePrice;

    @SerializedName("has_discount")
    private boolean hasDiscount;

    @SerializedName("is_wishlisted")
    private boolean isWishlisted;

    @SerializedName("is_mine")
    private boolean isMine;

    @SerializedName("discount_percent")
    private Double discountPercent;

    @SerializedName("distance")
    private Double distanceKm;

    @SerializedName("avg_rating")
    private double avgRating;

    @SerializedName("review_count")
    private int reviewCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getOriginalPrice() {
        return price; // In the database, `price` is the original price before discount
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getStock() {
        return stock;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public double getEffectivePrice() {
        return effectivePrice;
    }

    public void setEffectivePrice(double effectivePrice) {
        this.effectivePrice = effectivePrice;
    }

    public boolean isHasDiscount() {
        return hasDiscount;
    }

    public void setHasDiscount(boolean hasDiscount) {
        this.hasDiscount = hasDiscount;
    }

    public void setDiscountPercent(Double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public boolean isWishlisted() {
        return isWishlisted;
    }

    public void setWishlisted(boolean wishlisted) {
        this.isWishlisted = wishlisted;
    }

    public boolean isMine() {
        return isMine;
    }

    public Double getDiscountPercent() {
        return discountPercent;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public String getCondition() {
        return condition;
    }

    public Integer getWeight() {
        return weight;
    }

    public String getLocation() {
        return location;
    }

    @SerializedName("user")
    private User user;

    public User getUser() {
        return user;
    }

    // Category
    private Category category;

    public Category getCategory() {
        return category;
    }

    public static class Category {
        private int id;
        private String name;
        private String slug;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSlug() {
            return slug;
        }
    }

    // Reviews
    private java.util.List<Review> reviews;

    public java.util.List<Review> getReviews() {
        return reviews;
    }

    // Product Images (multiple)
    private java.util.List<ProductImage> images;

    public java.util.List<ProductImage> getImages() {
        return images;
    }

    /**
     * Build full image URLs from the images array.
     * Falls back to the single 'image' field if images is empty.
     */
    public java.util.List<String> getImageUrls(String baseStorageUrl) {
        java.util.List<String> urls = new java.util.ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (ProductImage img : images) {
                urls.add(baseStorageUrl + img.getImagePath());
            }
        } else if (image != null && !image.isEmpty()) {
            urls.add(image);
        }
        return urls;
    }

    public static class ProductImage {
        private int id;
        @SerializedName("image_path")
        private String imagePath;

        public int getId() {
            return id;
        }

        public String getImagePath() {
            return imagePath;
        }
    }
}
