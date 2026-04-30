package com.octania.marketplace.ui.product;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityAddProductBinding;
import com.octania.marketplace.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddProductActivity extends AppCompatActivity {
    private ActivityAddProductBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;
    private SessionManager sessionManager;

    private Double currentLat = null;
    private Double currentLng = null;
    private List<Uri> selectedImages = new ArrayList<>();
    private ImageAdapter imageAdapter;

    private List<String> categoryNames = new ArrayList<>();
    private List<Integer> categoryIds = new ArrayList<>();

    private final String[] conditionList = { "Baru", "Bekas" };

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                            if (selectedImages.size() < 6)
                                selectedImages.add(imageUri);
                        }
                    } else if (result.getData().getData() != null) {
                        if (selectedImages.size() < 6)
                            selectedImages.add(result.getData().getData());
                    }
                    imageAdapter.notifyDataSetChanged();
                }
            });

    private Uri currentCameraPhotoUri;
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && currentCameraPhotoUri != null) {
                    if (selectedImages.size() < 6) {
                        selectedImages.add(currentCameraPhotoUri);
                        imageAdapter.notifyDataSetChanged();
                    }
                }
            });

    private final String[] weightUnits = { "gr", "kg" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        imageAdapter = new ImageAdapter(this, selectedImages);
        binding.rvImages.setAdapter(imageAdapter);

        // Fetch categories dynamically
        fetchCategories();

        ArrayAdapter<String> condAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                conditionList);
        binding.spinnerCondition.setAdapter(condAdapter);

        // Setup weight units spinner
        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                weightUnits);
        binding.spinnerWeightUnit.setAdapter(weightAdapter);

        binding.btnSelectImage.setOnClickListener(v -> openImageChooser());
        binding.btnSubmit.setOnClickListener(v -> submitProduct());

        // Handle location icon click
        binding.tilLocation.setEndIconOnClickListener(v -> fetchLocation());

        fetchLocation();
        setupBottomNav();
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_add);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, com.octania.marketplace.ui.home.HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, com.octania.marketplace.ui.seller.SellerOrdersActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                return true;
            } else if (id == R.id.nav_wishlist) {
                startActivity(new Intent(this, com.octania.marketplace.ui.profile.WishlistActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, com.octania.marketplace.ui.profile.ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void fetchCategories() {
        apiService.getCategories().enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = new com.google.gson.Gson().toJson(response.body().getData());
                        org.json.JSONArray arr = new org.json.JSONArray(json);
                        categoryNames.clear();
                        categoryIds.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            org.json.JSONObject obj = arr.getJSONObject(i);
                            categoryIds.add(obj.getInt("id"));
                            categoryNames.add(obj.getString("name"));
                        }
                        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(AddProductActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, categoryNames);
                        binding.spinnerCategory.setAdapter(catAdapter);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
            }
        });
    }

    private void openImageChooser() {
        CharSequence[] options = { "Ambil Gambar Langsung (Kamera)", "Pilih Gambar (Galeri)" };
        new android.app.AlertDialog.Builder(this)
                .setTitle("Pilih Foto (Maksimal 6)")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        try {
                            File photoFile = File.createTempFile("photo_", ".jpg",
                                    getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES));
                            currentCameraPhotoUri = androidx.core.content.FileProvider.getUriForFile(this,
                                    getApplicationContext().getPackageName() + ".provider", photoFile);
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentCameraPhotoUri);
                            cameraLauncher.launch(intent);
                        } catch (IOException ex) {
                            Toast.makeText(this, "Gagal memuat kamera", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        imagePickerLauncher.launch(Intent.createChooser(intent, "Pilih Gambar"));
                    }
                }).show();
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        binding.tvLocationStatus.setText("Mencari lokasi otomatis Anda...");
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                binding.tvLocationStatus.setText("Lokasi ditemukan siap dipasang!");

                // Use Geocoder to get address
                try {
                    android.location.Geocoder geocoder = new android.location.Geocoder(this,
                            java.util.Locale.getDefault());
                    List<android.location.Address> addresses = geocoder.getFromLocation(currentLat, currentLng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        String address = addresses.get(0).getAddressLine(0);
                        binding.etLocation.setText(address);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                binding.tvLocationStatus.setText("Lokasi tidak terbaca perangkat.");
            }
        }).addOnFailureListener(e -> binding.tvLocationStatus.setText("Gagal mendapat lokasi: " + e.getMessage()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk jual produk area sekitar", Toast.LENGTH_SHORT).show();
            binding.tvLocationStatus.setText("Izin lokasi ditolak");
        }
    }

    private void submitProduct() {
        String name = binding.etName.getText().toString().trim();
        String price = binding.etPrice.getText().toString().trim();
        String discountPriceText = binding.etDiscountPrice.getText().toString().trim();
        String stock = binding.etStock.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();

        String condition = binding.spinnerCondition.getSelectedItem().toString();

        int categoryIndex = binding.spinnerCategory.getSelectedItemPosition();
        if (categoryIndex == -1 || categoryIds.isEmpty()) {
            Toast.makeText(this, "Kategori belum dimuat atau kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        String categoryId = String.valueOf(categoryIds.get(categoryIndex));

        String weightInput = binding.etWeight.getText().toString().trim();
        String weightUnit = binding.spinnerWeightUnit.getSelectedItem().toString();
        String finalWeight = weightInput;

        if (!weightInput.isEmpty()) {
            try {
                double w = Double.parseDouble(weightInput);
                if ("kg".equals(weightUnit)) {
                    w = w * 1000;
                }
                finalWeight = String.valueOf((int) w);
            } catch (Exception e) {
                finalWeight = weightInput;
            }
        }

        String location = binding.etLocation.getText().toString().trim();

        if (name.isEmpty() || price.isEmpty() || stock.isEmpty() || desc.isEmpty() || weightInput.isEmpty()
                || location.isEmpty() || selectedImages.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi semua data dan minimal 1 foto!", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmit.setEnabled(false);
        binding.btnSubmit.setText("Mengunggah...");

        try {
            List<MultipartBody.Part> imageParts = new ArrayList<>();
            for (Uri uri : selectedImages) {
                File imageFile = createTempFileFromUri(uri);
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData("images[]", imageFile.getName(),
                        requestFile);
                imageParts.add(imagePart);
            }

            RequestBody nameBody = RequestBody.create(MediaType.parse("text/plain"), name);
            RequestBody descBody = RequestBody.create(MediaType.parse("text/plain"), desc);
            RequestBody priceBody = RequestBody.create(MediaType.parse("text/plain"), price);
            RequestBody discountBody = RequestBody.create(MediaType.parse("text/plain"),
                    discountPriceText.isEmpty() ? "" : discountPriceText);
            RequestBody stockBody = RequestBody.create(MediaType.parse("text/plain"), stock);
            RequestBody categoryIdBody = RequestBody.create(MediaType.parse("text/plain"), categoryId);
            RequestBody conditionBody = RequestBody.create(MediaType.parse("text/plain"), condition);
            RequestBody weightBody = RequestBody.create(MediaType.parse("text/plain"), finalWeight);
            RequestBody locationBody = RequestBody.create(MediaType.parse("text/plain"), location);
            // Jika lokasi GPS tidak tersedia, kirim string kosong agar backend tetap menerima request.
            String latString = currentLat != null ? String.valueOf(currentLat) : "";
            String lngString = currentLng != null ? String.valueOf(currentLng) : "";
            RequestBody latBody = RequestBody.create(MediaType.parse("text/plain"), latString);
            RequestBody lngBody = RequestBody.create(MediaType.parse("text/plain"), lngString);

            apiService.addProduct("Bearer " + sessionManager.getToken(),
                    nameBody, descBody, priceBody, discountBody, stockBody, categoryIdBody, conditionBody, weightBody,
                    locationBody, latBody, lngBody, imageParts)
                    .enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                            binding.btnSubmit.setEnabled(true);
                            binding.btnSubmit.setText("Simpan & Jual");

                            if (response.isSuccessful()) {
                                Toast.makeText(AddProductActivity.this, "Berhasil diunggah!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(AddProductActivity.this,
                                        com.octania.marketplace.ui.home.HomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("refresh_from_add_product", true);
                                startActivity(intent);
                                finishAffinity();
                            } else {
                                Toast.makeText(AddProductActivity.this, "Gagal mengunggah kode: " + response.code(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            binding.btnSubmit.setEnabled(true);
                            binding.btnSubmit.setText("Simpan & Jual");
                            Toast.makeText(AddProductActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });

        } catch (Exception e) {
            binding.btnSubmit.setEnabled(true);
            binding.btnSubmit.setText("Simpan & Jual");
            Toast.makeText(this, "File error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
        File tempFile = File.createTempFile("product_img_", ".jpg", getCacheDir());
        FileOutputStream out = new FileOutputStream(tempFile);

        if (bitmap != null) {
            int maxResolution = 1000;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxResolution || height > maxResolution) {
                float ratio = Math.min((float) maxResolution / width, (float) maxResolution / height);
                width = Math.round(ratio * width);
                height = Math.round(ratio * height);
            }
            android.graphics.Bitmap resized = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true);
            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out);
        } else {
            // Fallback
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }

        out.close();
        if (inputStream != null)
            inputStream.close();
        return tempFile;
    }

    private static class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private final List<Uri> images;
        private final Context context;

        public ImageAdapter(Context context, List<Uri> images) {
            this.context = context;
            this.images = images;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Uri imageUri = images.get(position);
            holder.imageView.setImageURI(imageUri);
            
            // Delete button click
            holder.btnDelete.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Hapus Gambar")
                    .setMessage("Apakah Anda yakin ingin menghapus gambar ini?")
                    .setPositiveButton("Hapus", (dialog, which) -> {
                        images.remove(position);
                        notifyDataSetChanged();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
            });
            
            // Image preview click
            holder.imageView.setOnClickListener(v -> {
                android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                android.view.View previewView = LayoutInflater.from(context).inflate(R.layout.dialog_image_preview, null);
                dialog.setContentView(previewView);

                ImageView ivPreview = previewView.findViewById(R.id.ivPreview);
                android.widget.Button btnClose = previewView.findViewById(R.id.btnClose);
                android.widget.Button btnDelete = previewView.findViewById(R.id.btnDeleteImage);

                ivPreview.setImageURI(imageUri);

                btnClose.setOnClickListener(dv -> dialog.dismiss());
                btnDelete.setOnClickListener(dv -> {
                    images.remove(position);
                    notifyDataSetChanged();
                    dialog.dismiss();
                });

                dialog.show();
            });
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ImageView btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.ivThumb);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.bottomNav != null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_add);
            com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);
        }
    }
}
