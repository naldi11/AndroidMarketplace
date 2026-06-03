package com.octania.marketplace.ui.profile;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.octania.marketplace.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

public class MapPickerActivity extends AppCompatActivity {
    private MapView mapView;
    private Button btnSelectLocation;
    private TextView tvSelectHint;
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;

    private EditText etSearchLocation;
    private ImageView btnClearSearch;
    private androidx.recyclerview.widget.RecyclerView rvSuggestions;

    private android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;
    private List<Address> suggestionList = new java.util.ArrayList<>();
    private SuggestionsAdapter suggestionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize OSMDroid configurations before setting content view
        android.content.SharedPreferences prefs = getSharedPreferences("osmdroid", 0);
        Configuration.getInstance().load(getApplicationContext(), prefs);

        setContentView(R.layout.activity_map_picker);

        mapView = findViewById(R.id.mapView);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        tvSelectHint = findViewById(R.id.tvSelectHint);

        etSearchLocation = findViewById(R.id.etSearchLocation);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        rvSuggestions = findViewById(R.id.rvSuggestions);

        ImageView btnBackSearch = findViewById(R.id.btnBackSearch);
        btnBackSearch.setOnClickListener(v -> {
            if (rvSuggestions.getVisibility() == View.VISIBLE) {
                rvSuggestions.setVisibility(View.GONE);
                hideKeyboard();
                etSearchLocation.clearFocus();
            } else {
                onBackPressed();
            }
        });

        rvSuggestions.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        suggestionsAdapter = new SuggestionsAdapter(suggestionList, new SuggestionsAdapter.OnSuggestionClickListener() {
            @Override
            public void onSuggestionClick(Address address) {
                GeoPoint target = new GeoPoint(address.getLatitude(), address.getLongitude());
                mapView.getController().animateTo(target);
                mapView.getController().setZoom(17.0);

                hideKeyboard();
                rvSuggestions.setVisibility(View.GONE);
                
                etSearchLocation.removeTextChangedListener(textWatcher);
                etSearchLocation.setText(address.getAddressLine(0));
                etSearchLocation.addTextChangedListener(textWatcher);
            }

            @Override
            public void onArrowClick(Address address) {
                etSearchLocation.removeTextChangedListener(textWatcher);
                etSearchLocation.setText(address.getAddressLine(0));
                etSearchLocation.setSelection(etSearchLocation.getText().length());
                etSearchLocation.addTextChangedListener(textWatcher);
            }
        });
        rvSuggestions.setAdapter(suggestionsAdapter);

        etSearchLocation.addTextChangedListener(textWatcher);
        etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearchLocation.getText().toString().trim();
                if (!query.isEmpty()) {
                    triggerSearch(query);
                }
                return true;
            }
            return false;
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearchLocation.setText("");
            rvSuggestions.setVisibility(View.GONE);
        });

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(16.0);
        // Default Center Point: Medan City example (fallback)
        GeoPoint startPoint = new GeoPoint(3.5900, 98.6700);
        mapController.setCenter(startPoint);

        btnSelectLocation.setOnClickListener(v -> resolveAddress());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 100);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLocation = location;
                    GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getController().animateTo(startPoint);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (rvSuggestions != null && rvSuggestions.getVisibility() == View.VISIBLE) {
            rvSuggestions.setVisibility(View.GONE);
            hideKeyboard();
            etSearchLocation.clearFocus();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    private void resolveAddress() {
        btnSelectLocation.setEnabled(false);
        tvSelectHint.setText("Sedang mencari alamat...");

        org.osmdroid.api.IGeoPoint mapCenter = mapView.getMapCenter();
        final double lat = mapCenter.getLatitude();
        final double lng = mapCenter.getLongitude();

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(MapPickerActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    // Extract full address lines
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        sb.append(address.getAddressLine(i)).append(", ");
                    }
                    final String fullAddress = sb.toString().trim();
                    String cleanAddress = fullAddress.endsWith(",") ? fullAddress.substring(0, fullAddress.length() - 1)
                            : fullAddress;

                    runOnUiThread(() -> {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("picked_address", cleanAddress);
                        resultIntent.putExtra("lat", lat);
                        resultIntent.putExtra("lng", lng);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MapPickerActivity.this, "Tidak dapat menemukan alamat di titik ini",
                                Toast.LENGTH_SHORT).show();
                        btnSelectLocation.setEnabled(true);
                        tvSelectHint.setText("Geser peta untuk menentukan posisi");
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MapPickerActivity.this, "Gagal meload Geocoder: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnSelectLocation.setEnabled(true);
                    tvSelectHint.setText("Geser peta untuk menentukan posisi");
                });
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    private final android.text.TextWatcher textWatcher = new android.text.TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() > 0) {
                btnClearSearch.setVisibility(android.view.View.VISIBLE);
            } else {
                btnClearSearch.setVisibility(android.view.View.GONE);
                rvSuggestions.setVisibility(android.view.View.GONE);
            }

            searchHandler.removeCallbacks(searchRunnable);
            final String query = s.toString().trim();
            searchRunnable = () -> {
                if (query.length() >= 2) {
                    triggerSearch(query);
                }
            };
            searchHandler.postDelayed(searchRunnable, 300);
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {}
    };

    private void triggerSearch(String query) {
        org.osmdroid.api.IGeoPoint mapCenter = mapView.getMapCenter();
        final double centerLat = mapCenter.getLatitude();
        final double centerLng = mapCenter.getLongitude();

        final double refLat = (userLocation != null) ? userLocation.getLatitude() : centerLat;
        final double refLng = (userLocation != null) ? userLocation.getLongitude() : centerLng;

        new Thread(() -> {
            try {
                // 1. Try Photon first (free, keyless, typo-tolerant, Elasticsearch-backed)
                List<Address> results = queryPhoton(query, centerLat, centerLng);

                // 2. Fallback to Nominatim if Photon is empty or failed
                if (results == null || results.isEmpty()) {
                    results = queryNominatim(query, centerLat, centerLng);
                }

                // 3. Fallback to Android Geocoder if both are empty or failed
                if (results == null || results.isEmpty()) {
                    Geocoder geocoder = new Geocoder(MapPickerActivity.this, Locale.getDefault());
                    double lowerLeftLat = centerLat - 1.5;
                    double lowerLeftLng = centerLng - 1.5;
                    double upperRightLat = centerLat + 1.5;
                    double upperRightLng = centerLng + 1.5;

                    results = geocoder.getFromLocationName(query, 15,
                            lowerLeftLat, lowerLeftLng, upperRightLat, upperRightLng);

                    if (results == null || results.isEmpty()) {
                        results = geocoder.getFromLocationName(query, 15);
                    }
                }

                // 4. Filter to keep only Indonesia locations (safety check)
                List<Address> filteredResults = new java.util.ArrayList<>();
                if (results != null) {
                    for (Address addr : results) {
                        String cCode = addr.getCountryCode();
                        String cName = addr.getCountryName();
                        
                        boolean isIndo = true;
                        if (cCode != null && !cCode.equalsIgnoreCase("ID")) {
                            isIndo = false;
                        }
                        if (cName != null && !cName.toLowerCase().contains("indonesia")) {
                            isIndo = false;
                        }
                        
                        if (isIndo) {
                            filteredResults.add(addr);
                        }
                    }
                }

                // 5. Sort results by distance ascending (closest first)
                java.util.Collections.sort(filteredResults, new java.util.Comparator<Address>() {
                    @Override
                    public int compare(Address a1, Address a2) {
                        float[] d1 = new float[1];
                        float[] d2 = new float[1];
                        Location.distanceBetween(refLat, refLng, a1.getLatitude(), a1.getLongitude(), d1);
                        Location.distanceBetween(refLat, refLng, a2.getLatitude(), a2.getLongitude(), d2);
                        return Float.compare(d1[0], d2[0]);
                    }
                });

                // 6. Limit display to maximum 6 closest results to avoid cluttering
                final List<Address> finalResults = filteredResults.size() > 6 
                        ? filteredResults.subList(0, 6) 
                        : filteredResults;

                runOnUiThread(() -> {
                    if (!finalResults.isEmpty()) {
                        suggestionList.clear();
                        suggestionList.addAll(finalResults);
                        suggestionsAdapter.setReferenceLocation(refLat, refLng);
                        suggestionsAdapter.notifyDataSetChanged();
                        rvSuggestions.setVisibility(android.view.View.VISIBLE);
                    } else {
                        rvSuggestions.setVisibility(android.view.View.GONE);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> rvSuggestions.setVisibility(android.view.View.GONE));
            }
        }).start();
    }

    private List<Address> queryPhoton(String query, double centerLat, double centerLng) {
        List<Address> results = new java.util.ArrayList<>();
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
            String urlStr = "https://photon.komoot.io/api/"
                    + "?q=" + encodedQuery
                    + "&limit=15"
                    + "&lat=" + centerLat
                    + "&lon=" + centerLng
                    + "&lang=id";
                    
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                java.io.InputStream in = new java.io.BufferedInputStream(conn.getInputStream());
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                
                org.json.JSONObject responseObj = new org.json.JSONObject(sb.toString());
                org.json.JSONArray features = responseObj.getJSONArray("features");
                for (int i = 0; i < features.length(); i++) {
                    org.json.JSONObject feat = features.getJSONObject(i);
                    org.json.JSONObject geometry = feat.getJSONObject("geometry");
                    org.json.JSONArray coords = geometry.getJSONArray("coordinates");
                    double lon = coords.getDouble(0);
                    double lat = coords.getDouble(1);
                    
                    org.json.JSONObject props = feat.getJSONObject("properties");
                    String name = props.optString("name", "");
                    String countrycode = props.optString("countrycode", "");
                    
                    // We only want results in Indonesia
                    if (!"ID".equalsIgnoreCase(countrycode)) {
                        continue;
                    }
                    
                    if (name.isEmpty()) {
                        continue;
                    }
                    
                    StringBuilder addressBuilder = new StringBuilder();
                    String street = props.optString("street", "");
                    String district = props.optString("district", "");
                    String city = props.optString("city", "");
                    String state = props.optString("state", "");
                    
                    if (!street.isEmpty()) addressBuilder.append(street).append(", ");
                    if (!district.isEmpty()) addressBuilder.append(district).append(", ");
                    if (!city.isEmpty()) addressBuilder.append(city).append(", ");
                    if (!state.isEmpty()) addressBuilder.append(state);
                    
                    String subtitle = addressBuilder.toString().trim();
                    if (subtitle.endsWith(",")) {
                        subtitle = subtitle.substring(0, subtitle.length() - 1);
                    }
                    
                    Address address = new Address(Locale.getDefault());
                    address.setLatitude(lat);
                    address.setLongitude(lon);
                    address.setFeatureName(name);
                    address.setAddressLine(0, subtitle);
                    address.setCountryCode("ID");
                    address.setCountryName("Indonesia");
                    results.add(address);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private List<Address> queryNominatim(String query, double centerLat, double centerLng) {
        List<Address> results = new java.util.ArrayList<>();
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
            
            // Viewport bounding box around map center (+/- 1.5 degrees is ~165km radius)
            double left = centerLng - 1.5;
            double right = centerLng + 1.5;
            double top = centerLat + 1.5;
            double bottom = centerLat - 1.5;
            
            String urlStr = "https://nominatim.openstreetmap.org/search"
                    + "?q=" + encodedQuery
                    + "&format=json"
                    + "&limit=15"
                    + "&accept-language=id"
                    + "&countrycodes=id"
                    + "&viewbox=" + left + "," + top + "," + right + "," + bottom;
                    
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                java.io.InputStream in = new java.io.BufferedInputStream(conn.getInputStream());
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                
                org.json.JSONArray jsonArray = new org.json.JSONArray(sb.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject obj = jsonArray.getJSONObject(i);
                    double lat = obj.getDouble("lat");
                    double lon = obj.getDouble("lon");
                    String displayName = obj.getString("display_name");
                    
                    Address address = new Address(Locale.getDefault());
                    address.setLatitude(lat);
                    address.setLongitude(lon);
                    
                    // Separate display_name into feature name (title) and address details
                    String[] parts = displayName.split(",", 2);
                    String title = parts[0].trim();
                    String subtitle = parts.length > 1 ? parts[1].trim() : "";
                    
                    address.setFeatureName(title);
                    address.setAddressLine(0, subtitle);
                    results.add(address);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private void hideKeyboard() {
        android.view.View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private static class SuggestionsAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> {
        private final List<Address> suggestions;
        private final OnSuggestionClickListener listener;
        private double refLat = 3.5900;
        private double refLng = 98.6700;

        public interface OnSuggestionClickListener {
            void onSuggestionClick(Address address);
            void onArrowClick(Address address);
        }

        public SuggestionsAdapter(List<Address> suggestions, OnSuggestionClickListener listener) {
            this.suggestions = suggestions;
            this.listener = listener;
        }

        public void setReferenceLocation(double lat, double lng) {
            this.refLat = lat;
            this.refLng = lng;
        }

        @androidx.annotation.NonNull
        @Override
        public ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggestion, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, int position) {
            Address address = suggestions.get(position);
            
            String title = address.getFeatureName();
            if (title == null || title.isEmpty()) {
                title = address.getThoroughfare();
            }
            if (title == null || title.isEmpty()) {
                title = address.getSubLocality();
            }
            if (title == null || title.isEmpty()) {
                title = address.getLocality();
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                sb.append(address.getAddressLine(i)).append(", ");
            }
            String fullAddress = sb.toString().trim();
            if (fullAddress.endsWith(",")) {
                fullAddress = fullAddress.substring(0, fullAddress.length() - 1);
            }
            if (fullAddress.isEmpty()) {
                fullAddress = "-";
            }

            // Calculate distance
            float[] results = new float[1];
            Location.distanceBetween(refLat, refLng, address.getLatitude(), address.getLongitude(), results);
            float distanceInMeters = results[0];
            float distanceInKm = distanceInMeters / 1000f;

            String distanceStr;
            if (distanceInKm < 1.0) {
                distanceStr = String.format(new Locale("in", "ID"), "%d m", (int) distanceInMeters);
            } else {
                distanceStr = String.format(new Locale("in", "ID"), "%.1f km", distanceInKm);
            }

            holder.tvTitle.setText(title);
            holder.tvSubtitle.setText(fullAddress);
            holder.tvDistance.setText(distanceStr);

            holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(address));
            holder.ivArrowUpRight.setOnClickListener(v -> listener.onArrowClick(address));
        }

        @Override
        public int getItemCount() {
            return suggestions.size();
        }

        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.TextView tvTitle;
            android.widget.TextView tvSubtitle;
            android.widget.TextView tvDistance;
            android.widget.ImageView ivArrowUpRight;

            public ViewHolder(@androidx.annotation.NonNull android.view.View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvSuggestionTitle);
                tvSubtitle = itemView.findViewById(R.id.tvSuggestionSubtitle);
                tvDistance = itemView.findViewById(R.id.tvSuggestionDistance);
                ivArrowUpRight = itemView.findViewById(R.id.ivArrowUpRight);
            }
        }
    }
}
