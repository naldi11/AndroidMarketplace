package com.octania.marketplace.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.User;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConversationsActivity extends AppCompatActivity {

    private RecyclerView rvConversations;
    private ConversationsAdapter adapter;
    private List<User> contactList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        initViews();
        loadConversations();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvConversations = findViewById(R.id.rvConversations);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        adapter = new ConversationsAdapter(contactList, contact -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("user_id", contact.getId());
            intent.putExtra("user_name", contact.getName());
            intent.putExtra("user_avatar", contact.getAvatar());
            startActivity(intent);
        });

        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadConversations);
    }

    private void loadConversations() {
        swipeRefresh.setRefreshing(true);
        String token = "Bearer " + sessionManager.getToken();
        apiService.getChatConversations(token).enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    contactList.clear();
                    contactList.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ConversationsActivity.this, "Gagal memuat percakapan", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations(); // Refresh list when returning to this screen
    }
}
