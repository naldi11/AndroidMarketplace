package com.octania.marketplace.ui.profile;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.User;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.ui.product.MyProductsActivity;
import com.octania.marketplace.ui.seller.MyOrdersActivity;
import com.octania.marketplace.ui.seller.SellerBalanceActivity;
import com.octania.marketplace.ui.seller.SellerOrdersActivity;
import com.octania.marketplace.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ProfileActivity — Shopee-style personal profile screen.
 * Shows user info, order shortcuts, seller tools, and account settings.
 */
public class ProfileActivity extends AppCompatActivity {

    private String getBaseStorageUrl() {
        return ApiClient.BASE_URL.replace("api/", "storage/");
    }

    private ApiService apiService;
    private SessionManager sessionManager;

    // Header views
    private ImageView ivAvatar;
    private FrameLayout btnChangeAvatar;
    private TextView tvUserName, tvUserEmail, tvUserPhone;
    private MaterialButton btnEditProfile;

    // Order shortcuts
    private LinearLayout btnOrderUnpaid, btnOrderPacked, btnOrderShipped, btnOrderReview;
    private TextView btnLihatSemua;

    // Seller menus
    private LinearLayout menuMyProducts, menuSellerBalance;

    // Settings menus
    private LinearLayout menuAddress, menuChangePassword;

    // Logout
    private MaterialButton btnLogout;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        uploadAvatar(selectedImageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        bindViews();
        setupClickListeners();
        setupBottomNav();
        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
            com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(bottomNav);
        }
        loadUserProfile();
    }

    // ===== View Binding =====

    private void bindViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        btnOrderUnpaid = findViewById(R.id.btnOrderUnpaid);
        btnOrderPacked = findViewById(R.id.btnOrderPacked);
        btnOrderShipped = findViewById(R.id.btnOrderShipped);
        btnOrderReview = findViewById(R.id.btnOrderReview);
        btnLihatSemua = findViewById(R.id.btnLihatSemua);

        menuMyProducts = findViewById(R.id.menuMyProducts);
        menuSellerBalance = findViewById(R.id.menuSellerBalance);

        menuAddress = findViewById(R.id.menuAddress);
        menuChangePassword = findViewById(R.id.menuChangePassword);

        btnLogout = findViewById(R.id.btnLogout);
    }

    // ===== Click Listeners =====

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        btnChangeAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // Order shortcuts — navigate to MyOrdersActivity with tab index
        btnLihatSemua.setOnClickListener(v -> openOrders(0));
        btnOrderUnpaid.setOnClickListener(v -> openOrders(1));
        btnOrderPacked.setOnClickListener(v -> openOrders(2));
        btnOrderShipped.setOnClickListener(v -> openOrders(3));
        btnOrderReview.setOnClickListener(v -> openOrders(4));

        // Seller menus
        menuMyProducts.setOnClickListener(v -> startActivity(new Intent(this, MyProductsActivity.class)));
        menuSellerBalance.setOnClickListener(v -> startActivity(new Intent(this, SellerBalanceActivity.class)));

        // Settings
        menuAddress.setOnClickListener(v -> startActivity(new Intent(this, ManageAddressActivity.class)));
        menuChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Logout
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    // ===== Bottom Navigation =====

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null)
            return;

        bottomNav.setSelectedItemId(R.id.nav_profile);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, com.octania.marketplace.ui.home.HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, SellerOrdersActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, com.octania.marketplace.ui.product.AddProductActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_wishlist) {
                startActivity(new Intent(this, WishlistActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    // ===== Navigation Helper =====

    private void openOrders(int tabIndex) {
        Intent intent = new Intent(this, MyOrdersActivity.class);
        intent.putExtra("tab_index", tabIndex);
        startActivity(intent);
    }

    // ===== Load User Profile =====

    private void loadUserProfile() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getUserProfile(token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderUserProfile(response.body());
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Gagal memuat profil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderUserProfile(User user) {
        tvUserName.setText(user.getName() != null ? user.getName() : "—");
        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "—");
        tvUserPhone.setText(user.getPhone() != null ? user.getPhone() : "—");

        String displayAvatar = null;
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            displayAvatar = user.getAvatar();
        } else if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            displayAvatar = user.getProfilePicture();
        }

        if (displayAvatar != null) {
            Glide.with(this)
                    .load(getBaseStorageUrl() + displayAvatar)
                    .circleCrop()
                    .placeholder(R.mipmap.ic_launcher_round)
                    .into(ivAvatar);
        }
    }

    private void uploadAvatar(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File file = new File(getCacheDir(), "avatar_temp.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("avatar", file.getName(), requestFile);
            String token = "Bearer " + sessionManager.getToken();

            // Check file size (2MB = 2 * 1024 * 1024 bytes)
            if (file.length() > 2 * 1024 * 1024) {
                Toast.makeText(this, "Ukuran file terlalu besar (Maksimal 2MB)", Toast.LENGTH_LONG).show();
                return;
            }

            apiService.updateAvatar(token, body).enqueue(new Callback<ApiResponse<User>>() {
                @Override
                public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Foto profil berhasil diperbarui ✅", Toast.LENGTH_SHORT)
                                .show();
                        loadUserProfile();
                    } else {
                        String errorMsg = "Gagal mengunggah foto";
                        try {
                            if (response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                ApiResponse<?> errorRes = new com.google.gson.Gson().fromJson(errorJson,
                                        ApiResponse.class);
                                if (errorRes.getMessage() != null) {
                                    errorMsg = errorRes.getMessage();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(ProfileActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                    Toast.makeText(ProfileActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Edit Profile Bottom Sheet =====

    private void showEditProfileDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        dialog.setContentView(view);

        TextInputEditText etName = view.findViewById(R.id.etName);
        TextInputEditText etEmail = view.findViewById(R.id.etEmail);
        TextInputEditText etPhone = view.findViewById(R.id.etPhone);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        // Pre-fill with current data (strip "—" placeholder)
        String currentName = tvUserName.getText().toString();
        String currentEmail = tvUserEmail.getText().toString();
        String currentPhone = tvUserPhone.getText().toString();
        etName.setText("—".equals(currentName) ? "" : currentName);
        etEmail.setText("—".equals(currentEmail) ? "" : currentEmail);
        etPhone.setText("—".equals(currentPhone) ? "" : currentPhone);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Nama dan email wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            updateProfile(name, email, phone, dialog);
        });

        dialog.show();
    }

    private void updateProfile(String name, String email, String phone, Dialog dialog) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.updateProfile(token, name, email, phone).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    Toast.makeText(ProfileActivity.this, "Profil berhasil diperbarui ✅", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadUserProfile();
                } else {
                    Toast.makeText(ProfileActivity.this, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===== Change Password Bottom Sheet =====

    private void showChangePasswordDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        dialog.setContentView(view);

        TextInputEditText etCurrent = view.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew = view.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = view.findViewById(R.id.etConfirmPassword);
        MaterialButton btnSave = view.findViewById(R.id.btnSavePassword);

        btnSave.setOnClickListener(v -> {
            String current = etCurrent.getText() != null ? etCurrent.getText().toString().trim() : "";
            String newPass = etNew.getText() != null ? etNew.getText().toString().trim() : "";
            String confirm = etConfirm.getText() != null ? etConfirm.getText().toString().trim() : "";

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Semua kolom wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPass.length() < 8) {
                Toast.makeText(this, "Password baru minimal 8 karakter", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirm)) {
                Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(current, newPass, confirm, dialog);
        });

        dialog.show();
    }

    private void changePassword(String current, String newPass, String confirm, Dialog dialog) {
        String token = "Bearer " + sessionManager.getToken();
        Map<String, String> body = new HashMap<>();
        body.put("current_password", current);
        body.put("password", newPass);
        body.put("password_confirmation", confirm);

        apiService.updatePassword(token, body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    Toast.makeText(ProfileActivity.this, "Password berhasil diubah ✅", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(ProfileActivity.this, "Password lama salah atau tidak valid", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===== Logout =====

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Keluar")
                .setMessage("Yakin ingin keluar dari akun?")
                .setPositiveButton("Keluar", (d, w) -> {
                    sessionManager.logoutUser();
                    Intent intent = new Intent(this, com.octania.marketplace.ui.auth.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
