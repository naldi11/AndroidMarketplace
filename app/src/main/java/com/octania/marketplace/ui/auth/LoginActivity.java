package com.octania.marketplace.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.octania.marketplace.data.model.AuthResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityLoginBinding;
import com.octania.marketplace.ui.home.HomeActivity;
import com.octania.marketplace.ui.seller.SellerDashboardActivity;
import com.octania.marketplace.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // Jika sudah login, cek role dan langsung ke Home/Dashboard
        if (sessionManager.isLoggedIn()) {
            String role = sessionManager.getActiveRole();
            if ("seller".equals(role)) {
                startActivity(new Intent(this, SellerDashboardActivity.class));
            } else {
                startActivity(new Intent(this, HomeActivity.class));
            }
            finish();
            return;
        }

        apiService = ApiClient.getClient().create(ApiService.class);

        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // Forgot password click handler
        binding.tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Fitur lupa password akan segera hadir", Toast.LENGTH_SHORT).show();
        });

        // Social login click handlers (placeholders)
        binding.cardGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Google Login - Coming Soon", Toast.LENGTH_SHORT).show();
        });

        binding.cardFacebook.setOnClickListener(v -> {
            Toast.makeText(this, "Facebook Login - Coming Soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void attemptLogin() {
        String loginId = binding.etLoginId.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Normalize phone number format
        loginId = normalizePhoneNumber(loginId);

        if (loginId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Harap isi Email/No HP dan Password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        apiService.login(loginId, password).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse res = response.body();
                    if ("success".equals(res.getStatus())) {
                        String token = res.getAccessToken();
                        int userId = res.getData().getId();

                        // Get selected role from server
                        String role = res.getData().getRole();
                        if (role == null || role.trim().isEmpty()) {
                            role = "buyer"; // fallback if null
                        }

                        sessionManager.createLoginSession(token, userId, role);

                        Toast.makeText(LoginActivity.this, "Login Berhasil", Toast.LENGTH_SHORT).show();
                        if ("seller".equals(role)) {
                            startActivity(new Intent(LoginActivity.this, com.octania.marketplace.ui.seller.SellerDashboardActivity.class));
                        } else {
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        }
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, res.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Gagal login, periksa kredensial Anda.", Toast.LENGTH_LONG)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Normalize phone number to standard format
     * Supports: 08xxx, 628xxx, +628xxx → converts to 628xxx
     */
    private String normalizePhoneNumber(String input) {
        if (input == null || input.isEmpty()) return input;
        
        // Remove spaces, dashes, and + sign
        String cleaned = input.replaceAll("[\\s\\-+]", "");
        
        // If starts with 0, replace with 62
        if (cleaned.startsWith("0")) {
            cleaned = "62" + cleaned.substring(1);
        }
        
        return cleaned;
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
        binding.etLoginId.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);
    }
}
