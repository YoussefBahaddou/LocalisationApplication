package com.emsi.localisationsmartphone;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private double latitude;
    private double longitude;
    private boolean locationAvailable = false;
    private RequestQueue requestQueue;
    private String insertUrl = "http://192.168.100.38/localisation/createPosition.php";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private Button saveLocationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        
        // Initialize the save button
        saveLocationButton = findViewById(R.id.saveLocationButton);
        saveLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationAvailable) {
                    addPosition(latitude, longitude);
                } else {
                    Toast.makeText(MapsActivity.this, R.string.no_location, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        requestQueue = Volley.newRequestQueue(this);

        // Check if we received location from MainActivity
        if (getIntent().hasExtra("latitude") && getIntent().hasExtra("longitude")) {
            latitude = getIntent().getDoubleExtra("latitude", 0);
            longitude = getIntent().getDoubleExtra("longitude", 0);
            locationAvailable = true;
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        
        // Start location updates
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        String[] permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Request more frequent updates for the map
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                locationAvailable = true;
                
                // Update map with new location
                updateMapLocation();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Not needed for this implementation
            }

            @Override
            public void onProviderEnabled(String provider) {
                // Not needed for this implementation
            }

            @Override
            public void onProviderDisabled(String provider) {
                // Not needed for this implementation
            }
        });
        
        // Try to get initial location
        getLastKnownLocation();
    }
    
    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastLocation != null && !locationAvailable) {
            latitude = lastLocation.getLatitude();
            longitude = lastLocation.getLongitude();
            locationAvailable = true;
            
            // Update map with last known location
            updateMapLocation();
        }
    }
    
    private void updateMapLocation() {
        if (mMap != null && locationAvailable) {
            LatLng currentLocation = new LatLng(latitude, longitude);
            
            // Clear previous markers
            mMap.clear();
            
            // Add a marker at the current location and move the camera
            mMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .title("Current Location"));
            
            // Move camera to the current location with zoom level 15
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
            
            // Enable my location button
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Configure map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        
        // Check if we have location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Enable the my-location layer
            mMap.setMyLocationEnabled(true);
        }
        
        // If we already have a location (from intent or last known), update the map
        if (locationAvailable) {
            updateMapLocation();
        }
        
        // Set up map click listener to allow user to select a location
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Update our current location variables
                latitude = latLng.latitude;
                longitude = latLng.longitude;
                locationAvailable = true;
                
                // Update the map with the selected location
                mMap.clear();
                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Selected Location"));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                
                Toast.makeText(MapsActivity.this, 
                        "Location selected: " + latitude + ", " + longitude, 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    void addPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(Request.Method.POST, insertUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Handle successful response
                        Toast.makeText(MapsActivity.this, R.string.location_saved_on_map, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                        String errorMsg = getString(R.string.location_send_error, 
                                error.getMessage() != null ? error.getMessage() : "Unknown error");
                        Toast.makeText(MapsActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return null;
                }
                
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                HashMap<String, String> params = new HashMap<String, String>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                params.put("latitude", lat + "");
                params.put("longitude", lon + "");
                params.put("date", sdf.format(new Date()) + "");
                
                // Note: getDeviceId() is deprecated, but used here as per the requirements
                // In a production app, you should use a more modern approach
                String imei = "unknown";
                try {
                    imei = telephonyManager.getDeviceId();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                params.put("imei", imei);
                
                return params;
            }
        };
        
        requestQueue.add(request);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Check if we have permissions when resuming
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Restart location updates if needed
            if (locationManager != null) {
                startLocationUpdates();
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates when app is in background to save battery
        if (locationManager != null && ActivityCompat.checkSelfPermission(this, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {}
                
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                
                @Override
                public void onProviderEnabled(@NonNull String provider) {}
                
                @Override
                public void onProviderDisabled(@NonNull String provider) {}
            });
        }
    }
}
