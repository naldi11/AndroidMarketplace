package com.octania.marketplace.ui.base;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.utils.SessionManager;
import com.octania.marketplace.utils.ToastManager;

/**
 * Base Activity class with common functionality
 * All activities should extend this class
 */
public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {
    
    protected VB binding;
    protected SessionManager sessionManager;
    protected ApiService apiService;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = getViewBinding();
        setContentView(binding.getRoot());
        
        sessionManager = new SessionManager(this);
        
        initViews();
        initListeners();
        observeData();
    }
    
    /**
     * Override to provide ViewBinding
     */
    protected abstract VB getViewBinding();
    
    /**
     * Initialize views - called in onCreate
     */
    protected abstract void initViews();
    
    /**
     * Initialize click listeners - called in onCreate
     */
    protected abstract void initListeners();
    
    /**
     * Observe LiveData or other data sources - called in onCreate
     */
    protected abstract void observeData();
    
    /**
     * Check if user is logged in, redirect to login if not
     */
    protected boolean requireAuth() {
        if (!sessionManager.isLoggedIn()) {
            showToast("Silakan login terlebih dahulu");
            // Navigate to login
            return false;
        }
        return true;
    }
    
    /**
     * Show toast message (with debounce to prevent spam)
     */
    protected void showToast(String message) {
        ToastManager.showToast(this, message);
    }
    
    /**
     * Show long toast message (with debounce)
     */
    protected void showLongToast(String message) {
        ToastManager.showLongToast(this, message);
    }
    
    /**
     * Show toast immediately without debounce (for critical messages)
     */
    protected void showToastImmediate(String message) {
        ToastManager.showToastImmediate(this, message);
    }
    
    /**
     * Set loading state for a view
     */
    protected void setLoading(boolean isLoading, View... viewsToDisable) {
        for (View view : viewsToDisable) {
            view.setEnabled(!isLoading);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
