package com.octania.marketplace.ui.chat;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.Message;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class ChatActivity extends AppCompatActivity {

    private int otherUserId;
    private String otherUserName;
    private String otherUserAvatar;

    private RecyclerView rvMessages;
    private ChatAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private EditText etMessage;
    private View btnSend;

    private ApiService apiService;
    private SessionManager sessionManager;
    private Handler pollingHandler = new Handler();
    private Runnable pollingRunnable;
    private static final int POLL_INTERVAL = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUserId = getIntent().getIntExtra("user_id", -1);
        otherUserName = getIntent().getStringExtra("user_name");
        otherUserAvatar = getIntent().getStringExtra("user_avatar");

        if (otherUserId == -1) {
            Toast.makeText(this, "User tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        initViews();
        setupChat();
        startPolling();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvName = findViewById(R.id.tvChatUserName);
        ImageView ivAvatar = findViewById(R.id.ivUserAvatar);
        tvName.setText(otherUserName != null ? otherUserName : "Chat");

        if (otherUserAvatar != null && !otherUserAvatar.isEmpty()) {
            String avatarUrl = ApiClient.BASE_URL.replace("api/", "storage/") + otherUserAvatar;
            Glide.with(this).load(avatarUrl).circleCrop().into(ivAvatar);
        }

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        btnSend.setOnClickListener(v -> sendMessage());
        
        findViewById(R.id.btnAttachImage).setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });
        
        findViewById(R.id.btnRemoveImage).setOnClickListener(v -> {
            removeSelectedImage();
        });
    }

    private void setupChat() {
        adapter = new ChatAdapter(messageList, sessionManager.getUserId());
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        loadMessages();
    }

    private void loadMessages() {
        loadMessages(false);
    }

    private void loadMessages(boolean forceRefresh) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getChatMessages(token, otherUserId).enqueue(new Callback<ApiResponse<List<Message>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Message>>> call, Response<ApiResponse<List<Message>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Message> newMessages = response.body().getData();
                    if (newMessages != null) {
                        // Always update if forced (after send), or only if count changed during polling
                        if (forceRefresh || newMessages.size() != adapter.getItemCount()) {
                            adapter.updateMessages(newMessages);
                            rvMessages.scrollToPosition(newMessages.size() - 1);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Message>>> call, Throwable t) {
                if (messageList.isEmpty()) {
                    Toast.makeText(ChatActivity.this, "Error memuat pesan", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty() && selectedImageUri == null) return;

        etMessage.setText("");
        String token = "Bearer " + sessionManager.getToken();
        
        RequestBody receiverIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(otherUserId));
        RequestBody messageBody = RequestBody.create(MediaType.parse("text/plain"), text);
        okhttp3.MultipartBody.Part attachmentPart = null;

        if (selectedImageUri != null) {
            try {
                java.io.File file = com.octania.marketplace.utils.ImageCompressor.compressImage(this, selectedImageUri);
                RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
                attachmentPart = okhttp3.MultipartBody.Part.createFormData("attachment", file.getName(), reqFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        apiService.sendMessage(token, receiverIdBody, messageBody, attachmentPart).enqueue(new Callback<ApiResponse<Message>>() {
            @Override
            public void onResponse(Call<ApiResponse<Message>> call, Response<ApiResponse<Message>> response) {
                if (response.isSuccessful()) {
                    removeSelectedImage();
                    loadMessages(true); // Force refresh after send
                } else {
                    Toast.makeText(ChatActivity.this, "Gagal mengirim pesan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Message>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Gagal mengirim, coba lagi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private android.net.Uri selectedImageUri = null;

    private final androidx.activity.result.ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    findViewById(R.id.layoutImagePreview).setVisibility(View.VISIBLE);
                    ImageView ivPreview = findViewById(R.id.ivImagePreview);
                    Glide.with(this).load(uri).into(ivPreview);
                }
            });

    private void removeSelectedImage() {
        selectedImageUri = null;
        findViewById(R.id.layoutImagePreview).setVisibility(View.GONE);
    }

    private void startPolling() {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                loadMessages();
                pollingHandler.postDelayed(this, POLL_INTERVAL);
            }
        };
        pollingHandler.postDelayed(pollingRunnable, POLL_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }
}
