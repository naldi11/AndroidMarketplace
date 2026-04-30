package com.octania.marketplace.data.local.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

import com.octania.marketplace.data.local.dao.ProductDao;
import com.octania.marketplace.data.local.dao.UserDao;
import com.octania.marketplace.data.local.entity.ProductEntity;
import com.octania.marketplace.data.local.entity.UserEntity;
import com.octania.marketplace.data.local.converter.DateConverter;

/**
 * Room Database for offline caching
 */
@Database(
    entities = {
        ProductEntity.class,
        UserEntity.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "marketplace_db";
    private static AppDatabase instance;
    
    public abstract ProductDao productDao();
    public abstract UserDao userDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build();
        }
        return instance;
    }
}
