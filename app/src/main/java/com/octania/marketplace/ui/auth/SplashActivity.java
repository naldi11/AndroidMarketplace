package com.octania.marketplace.ui.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.octania.marketplace.R;
import com.octania.marketplace.databinding.ActivitySplashBinding;
import com.octania.marketplace.ui.home.HomeActivity;
import com.octania.marketplace.ui.seller.SellerDashboardActivity;
import com.octania.marketplace.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private SessionManager sessionManager;
    private Handler handler;
    private Runnable navigationRunnable;

    // Animation durations
    private static final long LOGO_ANIMATION_DURATION = 800;
    private static final long TEXT_ANIMATION_DURATION = 600;
    private static final long PROGRESS_ANIMATION_DURATION = 400;
    private static final long NAVIGATION_DELAY = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize view binding
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize session manager
        sessionManager = new SessionManager(this);
        handler = new Handler(Looper.getMainLooper());

        // Set version text
        String versionName = getAppVersion();
        binding.tvVersion.setText(versionName);

        // Start animations
        startAnimations();

        // Schedule navigation
        scheduleNavigation();
    }

    private void startAnimations() {
        // Step 1: Logo scale and fade animation
        animateLogo();

        // Step 2: App name and tagline fade in
        handler.postDelayed(this::animateText, LOGO_ANIMATION_DURATION - 200);

        // Step 3: Progress indicator animation
        handler.postDelayed(this::animateProgress, 
            LOGO_ANIMATION_DURATION + TEXT_ANIMATION_DURATION - 300);
    }

    private void animateLogo() {
        // Scale animation: from 0.7 to 1.0
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.cardLogo, "scaleX", 0.7f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.cardLogo, "scaleY", 0.7f, 1.0f);
        
        // Fade in animation
        ObjectAnimator alpha = ObjectAnimator.ofFloat(binding.cardLogo, "alpha", 0f, 1f);
        
        // Elevation animation
        ObjectAnimator elevation = ObjectAnimator.ofFloat(binding.cardLogo, "cardElevation", 0f, 16f);

        // Combine animations
        AnimatorSet logoAnimator = new AnimatorSet();
        logoAnimator.playTogether(scaleX, scaleY, alpha, elevation);
        logoAnimator.setDuration(LOGO_ANIMATION_DURATION);
        logoAnimator.setInterpolator(new AnticipateOvershootInterpolator(1.5f));
        logoAnimator.start();

        // Add pulse animation after initial animation
        handler.postDelayed(this::startPulseAnimation, LOGO_ANIMATION_DURATION + 500);
    }

    private void startPulseAnimation() {
        if (binding == null) return;
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.cardLogo, "scaleX", 1.0f, 1.05f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.cardLogo, "scaleY", 1.0f, 1.05f, 1.0f);
        
        AnimatorSet pulse = new AnimatorSet();
        pulse.playTogether(scaleX, scaleY);
        pulse.setDuration(1000);
        pulse.setInterpolator(new DecelerateInterpolator());
        pulse.start();
    }

    private void animateText() {
        if (binding == null) return;
        // Animate App Name
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(binding.tvAppName, "alpha", 0f, 1f);
        ObjectAnimator nameTranslation = ObjectAnimator.ofFloat(binding.tvAppName, "translationY", 20f, 0f);
        
        AnimatorSet nameSet = new AnimatorSet();
        nameSet.playTogether(nameAlpha, nameTranslation);
        nameSet.setDuration(TEXT_ANIMATION_DURATION);
        nameSet.setInterpolator(new DecelerateInterpolator());
        nameSet.start();

        // Animate Tagline (slight delay)
        handler.postDelayed(() -> {
            if (binding == null) return;
            ObjectAnimator taglineAlpha = ObjectAnimator.ofFloat(binding.tvTagline, "alpha", 0f, 1f);
            ObjectAnimator taglineTranslation = ObjectAnimator.ofFloat(binding.tvTagline, "translationY", 15f, 0f);
            
            AnimatorSet taglineSet = new AnimatorSet();
            taglineSet.playTogether(taglineAlpha, taglineTranslation);
            taglineSet.setDuration(TEXT_ANIMATION_DURATION);
            taglineSet.setInterpolator(new DecelerateInterpolator());
            taglineSet.start();
        }, 150);
    }

    private void animateProgress() {
        if (binding == null) return;
        // Progress bar fade in
        ObjectAnimator progressAlpha = ObjectAnimator.ofFloat(binding.progressBar, "alpha", 0f, 1f);
        progressAlpha.setDuration(PROGRESS_ANIMATION_DURATION);
        progressAlpha.start();

        // Loading text fade in
        ObjectAnimator loadingAlpha = ObjectAnimator.ofFloat(binding.tvLoading, "alpha", 0f, 1f);
        loadingAlpha.setDuration(PROGRESS_ANIMATION_DURATION);
        loadingAlpha.start();

        // Animate progress bar value
        ValueAnimator progressAnimator = ValueAnimator.ofInt(0, 100);
        progressAnimator.setDuration(NAVIGATION_DELAY - LOGO_ANIMATION_DURATION - TEXT_ANIMATION_DURATION);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            binding.progressBar.setProgress(progress);
        });
        progressAnimator.start();
    }

    private void scheduleNavigation() {
        navigationRunnable = () -> {
            navigateToNextScreen();
        };
        handler.postDelayed(navigationRunnable, NAVIGATION_DELAY);
    }

    private void navigateToNextScreen() {
        Intent intent;
        
        if (sessionManager.isLoggedIn()) {
            // User already logged in, check role
            String role = sessionManager.getActiveRole();
            if ("seller".equals(role)) {
                intent = new Intent(SplashActivity.this, SellerDashboardActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, HomeActivity.class);
            }
        } else {
            // User not logged in, go to Login
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        // Clear activity stack and start new activity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        // Apply transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        
        finish();
    }

    private String getAppVersion() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return "v" + versionName;
        } catch (Exception e) {
            return "v1.0.0";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handlers and animations
        if (handler != null && navigationRunnable != null) {
            handler.removeCallbacks(navigationRunnable);
        }
        binding = null;
    }

    @Override
    public void onBackPressed() {
        // Prevent back press on splash screen
        // Do nothing or show exit confirmation
    }
}
