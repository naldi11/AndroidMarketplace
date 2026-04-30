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

import com.octania.marketplace.ui.profile.MapPickerActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    private Double lat = null;
    private Double lng = null;

    private final ActivityResultLauncher<Intent> mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String address = result.getData().getStringExtra("picked_address");
                    lat = result.getData().getDoubleExtra("lat", 0);
                    lng = result.getData().getDoubleExtra("lng", 0);
                    binding.etAddress.setText(address);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        // Setup toolbar navigation
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Setup Map Picker Icon
        binding.tilAddress.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            mapLauncher.launch(intent);
        });

        // Setup password strength indicator
        binding.etPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        binding.btnRegister.setOnClickListener(v -> attemptRegister());

        binding.tvLogin.setOnClickListener(v -> finish());

        binding.tvTermsLink.setOnClickListener(v -> showTermsDialog());

        binding.rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isSeller = checkedId == com.octania.marketplace.R.id.rbSeller;
            binding.llSellerFields.setVisibility(isSeller ? View.VISIBLE : View.GONE);
        });
    }

    private void updatePasswordStrength(String password) {
        int strength = calculatePasswordStrength(password);
        
        // Reset all indicators
        binding.indicatorWeak.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        binding.indicatorMedium.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        binding.indicatorStrong.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        
        String strengthText;
        int strengthColor;
        
        switch (strength) {
            case 0:
            case 1:
                strengthText = "Kekuatan: Lemah";
                strengthColor = getResources().getColor(android.R.color.holo_red_light);
                binding.indicatorWeak.setBackgroundColor(strengthColor);
                break;
            case 2:
                strengthText = "Kekuatan: Sedang";
                strengthColor = getResources().getColor(android.R.color.holo_orange_light);
                binding.indicatorWeak.setBackgroundColor(strengthColor);
                binding.indicatorMedium.setBackgroundColor(strengthColor);
                break;
            case 3:
                strengthText = "Kekuatan: Kuat";
                strengthColor = getResources().getColor(android.R.color.holo_green_light);
                binding.indicatorWeak.setBackgroundColor(strengthColor);
                binding.indicatorMedium.setBackgroundColor(strengthColor);
                binding.indicatorStrong.setBackgroundColor(strengthColor);
                break;
            default:
                strengthText = "Kekuatan password";
                strengthColor = getResources().getColor(android.R.color.darker_gray);
        }
        
        binding.tvPasswordStrength.setText(strengthText);
        binding.tvPasswordStrength.setTextColor(strengthColor);
    }

    private int calculatePasswordStrength(String password) {
        int strength = 0;
        
        if (password.length() >= 8) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) strength++;
        
        return Math.min(strength, 3);
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

        int selectedRoleId = binding.rgRole.getCheckedRadioButtonId();
        String role = selectedRoleId == com.octania.marketplace.R.id.rbSeller ? "seller" : "buyer";
        String shopName = binding.etShopName.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("seller".equals(role)) {
            if (shopName.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Nama dan Alamat Toko wajib diisi untuk Penjual!", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            shopName = null;
            address = null;
            lat = null;
            lng = null;
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

        apiService.register(name, email, phone, password, confirmPassword, role, shopName, address, lat, lng).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse res = response.body();
                    if ("success".equals(res.getStatus())) {
                        String token = res.getAccessToken();
                        int userId = res.getData().getId();
                        String responseRole = res.getData().getRole();
                        if (responseRole == null) {
                            responseRole = role; // fallback
                        }

                        sessionManager.createLoginSession(token, userId, responseRole);

                        Toast.makeText(RegisterActivity.this, "Pendaftaran berhasil!", Toast.LENGTH_SHORT).show();

                        Intent intent;
                        if ("seller".equals(responseRole)) {
                            intent = new Intent(RegisterActivity.this, com.octania.marketplace.ui.seller.SellerDashboardActivity.class);
                        } else {
                            intent = new Intent(RegisterActivity.this, HomeActivity.class);
                        }
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
        binding.etShopName.setEnabled(!isLoading);
        binding.etAddress.setEnabled(!isLoading);
        for(int i = 0; i < binding.rgRole.getChildCount(); i++){
            binding.rgRole.getChildAt(i).setEnabled(!isLoading);
        }
        binding.etPassword.setEnabled(!isLoading);
        binding.etPasswordConfirm.setEnabled(!isLoading);
    }
}
