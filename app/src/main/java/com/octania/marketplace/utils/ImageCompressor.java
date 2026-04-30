package com.octania.marketplace.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for image compression before upload
 * Uses quality and dimension compression to reduce file size
 */
public class ImageCompressor {
    
    // Default compression settings
    private static final int DEFAULT_MAX_WIDTH = 1024;
    private static final int DEFAULT_MAX_HEIGHT = 1024;
    private static final int DEFAULT_QUALITY = 85;
    private static final long MAX_FILE_SIZE_BYTES = 1024 * 1024; // 1MB
    
    /**
     * Compress image from URI with default settings
     * @param context Application context
     * @param imageUri Source image URI
     * @return Compressed image as File
     */
    public static File compressImage(Context context, Uri imageUri) throws IOException {
        return compressImage(context, imageUri, DEFAULT_MAX_WIDTH, DEFAULT_MAX_HEIGHT, DEFAULT_QUALITY);
    }
    
    /**
     * Compress image with custom settings
     * @param context Application context
     * @param imageUri Source image URI
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @param quality JPEG quality (0-100)
     * @return Compressed image as File
     */
    public static File compressImage(Context context, Uri imageUri, int maxWidth, int maxHeight, int quality) throws IOException {
        // Get original bitmap dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        BitmapFactory.decodeStream(inputStream, null, options);
        if (inputStream != null) inputStream.close();
        
        int originalWidth = options.outWidth;
        int originalHeight = options.outHeight;
        
        // Calculate sample size to reduce memory usage
        int sampleSize = calculateSampleSize(originalWidth, originalHeight, maxWidth, maxHeight);
        
        // Decode bitmap with sample size
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        
        inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        if (inputStream != null) inputStream.close();
        
        if (bitmap == null) {
            throw new IOException("Failed to decode image");
        }
        
        // Scale down if still too large
        Bitmap scaledBitmap = scaleBitmap(bitmap, maxWidth, maxHeight);
        if (scaledBitmap != bitmap) {
            bitmap.recycle();
        }
        
        // Compress and save to file
        File compressedFile = createTempFile(context);
        FileOutputStream fos = new FileOutputStream(compressedFile);
        
        // Try to achieve target file size
        int currentQuality = quality;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, baos);
        
        // If still too large, reduce quality
        while (baos.size() > MAX_FILE_SIZE_BYTES && currentQuality > 50) {
            baos.reset();
            currentQuality -= 5;
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, baos);
        }
        
        // Write to file
        fos.write(baos.toByteArray());
        fos.flush();
        fos.close();
        
        scaledBitmap.recycle();
        
        return compressedFile;
    }
    
    /**
     * Calculate sample size for BitmapFactory
     */
    private static int calculateSampleSize(int originalWidth, int originalHeight, int maxWidth, int maxHeight) {
        int sampleSize = 1;
        
        if (originalHeight > maxHeight || originalWidth > maxWidth) {
            final int halfHeight = originalHeight / 2;
            final int halfWidth = originalWidth / 2;
            
            while ((halfHeight / sampleSize) >= maxHeight && (halfWidth / sampleSize) >= maxWidth) {
                sampleSize *= 2;
            }
        }
        
        return sampleSize;
    }
    
    /**
     * Scale bitmap maintaining aspect ratio
     */
    private static Bitmap scaleBitmap(Bitmap source, int maxWidth, int maxHeight) {
        int width = source.getWidth();
        int height = source.getHeight();
        
        if (width <= maxWidth && height <= maxHeight) {
            return source;
        }
        
        float aspectRatio = (float) width / height;
        
        if (width > height) {
            width = maxWidth;
            height = (int) (width / aspectRatio);
        } else {
            height = maxHeight;
            width = (int) (height * aspectRatio);
        }
        
        return Bitmap.createScaledBitmap(source, width, height, true);
    }
    
    /**
     * Create temporary file for compressed image
     */
    private static File createTempFile(Context context) throws IOException {
        String fileName = "compressed_" + System.currentTimeMillis() + ".jpg";
        File cacheDir = new File(context.getCacheDir(), "compressed_images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return new File(cacheDir, fileName);
    }
    
    /**
     * Clean up old compressed files
     */
    public static void clearCompressedCache(Context context) {
        File cacheDir = new File(context.getCacheDir(), "compressed_images");
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }
    
    /**
     * Get compressed image size in KB
     */
    public static long getFileSizeInKB(File file) {
        return file.length() / 1024;
    }
    
    /**
     * Async task for compression with callback
     */
    public interface CompressionCallback {
        void onSuccess(File compressedFile, long originalSize, long compressedSize);
        void onError(Exception e);
    }
    
    public static void compressAsync(Context context, Uri imageUri, CompressionCallback callback) {
        new AsyncTask<Void, Void, File>() {
            private Exception error;
            private long originalSize;
            
            @Override
            protected File doInBackground(Void... voids) {
                try {
                    // Get original file size
                    InputStream is = context.getContentResolver().openInputStream(imageUri);
                    if (is != null) {
                        originalSize = is.available();
                        is.close();
                    }
                    
                    return compressImage(context, imageUri);
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(File compressedFile) {
                if (error != null) {
                    callback.onError(error);
                } else if (compressedFile != null) {
                    long compressedSize = compressedFile.length();
                    callback.onSuccess(compressedFile, originalSize, compressedSize);
                }
            }
        }.execute();
    }
}
