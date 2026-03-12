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

public class MapPickerActivity extends AppCompatActivity {
    private MapView mapView;
    private Button btnSelectLocation;
    private TextView tvSelectHint;
    private FusedLocationProviderClient fusedLocationClient;

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
                    GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getController().animateTo(startPoint);
                }
            });
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
}
