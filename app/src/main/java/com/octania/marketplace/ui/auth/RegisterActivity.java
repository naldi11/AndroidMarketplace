package com.octania.marketplace.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.AuthResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityRegisterBinding;
import com.octania.marketplace.ui.home.HomeActivity;
import com.octania.marketplace.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        binding.btnRegister.setOnClickListener(v -> attemptRegister());

        binding.tvLogin.setOnClickListener(v -> finish());

        binding.tvTermsLink.setOnClickListener(v -> showTermsDialog());
    }

    private void showTermsDialog() {
        setLoading(true);
        apiService.getSettingByKey("terms_and_conditions")
                .enqueue(new Callback<ApiResponse<java.util.Map<String, String>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<java.util.Map<String, String>>> call,
                            Response<ApiResponse<java.util.Map<String, String>>> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            String content = response.body().getData().get("value");
                            new androidx.appcompat.app.AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("Syarat & Ketentuan")
                                    .setMessage(content)
                                    .setPositiveButton("Tutup", null)
                                    .show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Gagal memuat S&K", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<java.util.Map<String, String>>> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(RegisterActivity.this, "Koneksi Error: " + t.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private void attemptRegister() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etPasswordConfirm.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!binding.cbTerms.isChecked()) {
            Toast.makeText(this, "Anda harus menyetujui Syarat & Ketentuan", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        apiService.register(name, email, phone, password, confirmPassword).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse res = response.body();
                    if ("success".equals(res.getStatus())) {
                        String token = res.getAccessToken();
                        int userId = res.getData().getId();

                        sessionManager.createLoginSession(token, userId);

                        Toast.makeText(RegisterActivity.this, "Pendaftaran berhasil!", Toast.LENGTH_SHORT).show();

                        // Clear task stack and go home
                        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, res.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorDisplay = "Pendaftaran gagal, periksa isian Anda.";
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            org.json.JSONObject jObjError = new org.json.JSONObject(errorStr);
                            if (jObjError.has("errors")) {
                                org.json.JSONObject errors = jObjError.getJSONObject("errors");
                                java.util.Iterator<String> keys = errors.keys();
                                if (keys.hasNext()) {
                                    errorDisplay = errors.getJSONArray(keys.next()).getString(0);
                                }
                            } else if (jObjError.has("message")) {
                                errorDisplay = jObjError.getString("message");
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    Toast.makeText(RegisterActivity.this, errorDisplay, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Koneksi Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!isLoading);
        binding.etName.setEnabled(!isLoading);
        binding.etEmail.setEnabled(!isLoading);
        binding.etPhone.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);
        binding.etPasswordConfirm.setEnabled(!isLoading);
    }
}
