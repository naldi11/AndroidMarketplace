package com.octania.marketplace.data.local.converter;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Room TypeConverter for Date objects
 */
public class DateConverter {
    
    @TypeConverter
    public static Date toDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }
    
    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }
}
