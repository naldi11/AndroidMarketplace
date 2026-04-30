package com.octania.marketplace.utils;

import android.text.TextUtils;
import android.util.Patterns;

import java.util.regex.Pattern;

/**
 * Input validation utilities for client-side validation
 * Prevents common security issues like injection attacks
 */
public class InputValidator {
    
    // Patterns for validation
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,13}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]{2,50}$");
    
    // SQL injection prevention - basic pattern
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|SCRIPT)\\b)|(--|#|\\/\\*|\\*\\/)",
        Pattern.CASE_INSENSITIVE
    );
    
    // XSS prevention pattern
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "<[^>]*>|javascript:|on\\w+\\s*=",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Validate email address
     */
    public static boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) return false;
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    /**
     * Validate phone number (Indonesian format)
     */
    public static boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) return false;
        // Remove +62 or 0 prefix for validation
        String cleanPhone = phone.replaceAll("[+\\s]", "").replaceFirst("^(62|0)", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }
    
    /**
     * Validate password (min 8 chars, 1 number, 1 uppercase, 1 lowercase)
     */
    public static boolean isValidPassword(String password) {
        if (TextUtils.isEmpty(password)) return false;
        return PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * Get password strength (0-3)
     */
    public static int getPasswordStrength(String password) {
        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        return strength;
    }
    
    /**
     * Validate name (letters and spaces only, 2-50 chars)
     */
    public static boolean isValidName(String name) {
        if (TextUtils.isEmpty(name)) return false;
        return NAME_PATTERN.matcher(name).matches();
    }
    
    /**
     * Check for potential SQL injection
     */
    public static boolean containsSqlInjection(String input) {
        if (TextUtils.isEmpty(input)) return false;
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }
    
    /**
     * Check for potential XSS
     */
    public static boolean containsXss(String input) {
        if (TextUtils.isEmpty(input)) return false;
        return XSS_PATTERN.matcher(input).find();
    }
    
    /**
     * Sanitize input by removing dangerous characters
     */
    public static String sanitizeInput(String input) {
        if (TextUtils.isEmpty(input)) return input;
        
        // Remove HTML tags
        String sanitized = input.replaceAll("<[^>]*>", "");
        
        // Remove SQL keywords
        sanitized = sanitized.replaceAll("(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|--|#)", "");
        
        // Remove JavaScript
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        
        // Remove event handlers
        sanitized = sanitized.replaceAll("(?i)on\\w+\\s*=", "");
        
        // Trim and limit length
        sanitized = sanitized.trim();
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 500);
        }
        
        return sanitized;
    }
    
    /**
     * Validate product search query
     */
    public static boolean isValidSearchQuery(String query) {
        if (TextUtils.isEmpty(query)) return false;
        if (query.length() < 2) return false;
        if (containsSqlInjection(query)) return false;
        if (containsXss(query)) return false;
        return true;
    }
    
    /**
     * Validate price (positive number, max 999,999,999)
     */
    public static boolean isValidPrice(double price) {
        return price > 0 && price <= 999999999;
    }
    
    /**
     * Validate stock quantity (positive integer, max 9999)
     */
    public static boolean isValidStock(int stock) {
        return stock >= 0 && stock <= 9999;
    }
    
    /**
     * Check if string is empty or null
     */
    public static boolean isEmpty(String input) {
        return TextUtils.isEmpty(input) || input.trim().isEmpty();
    }
    
    /**
     * Get validation error message
     */
    public static String getValidationError(String field, String value, String type) {
        switch (type) {
            case "email":
                return isValidEmail(value) ? null : "Format email tidak valid";
            case "phone":
                return isValidPhone(value) ? null : "Format nomor HP tidak valid (10-13 digit)";
            case "password":
                return isValidPassword(value) ? null : "Password minimal 8 karakter dengan huruf besar, kecil, dan angka";
            case "name":
                return isValidName(value) ? null : "Nama hanya boleh huruf dan spasi (2-50 karakter)";
            case "required":
                return isEmpty(value) ? field + " wajib diisi" : null;
            default:
                return null;
        }
    }
}
