package com.emsi.localisationsmartphone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;
    private boolean locationAvailable = false;
    
    RequestQueue requestQueue;
    String insertUrl = "http://192.168.100.38/localisation/createPosition.php";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView altitudeTextView;
    private TextView accuracyTextView;
    private TextView statusTextView;
    private Button sendLocationButton;
    private Button viewMapButton;
    private LocationManager locationManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        latitudeTextView = findViewById(R.id.latitudeTextView);
        longitudeTextView = findViewById(R.id.longitudeTextView);
        altitudeTextView = findViewById(R.id.altitudeTextView);
        accuracyTextView = findViewById(R.id.accuracyTextView);
        statusTextView = findViewById(R.id.statusTextView);
        sendLocationButton = findViewById(R.id.sendLocationButton);
        viewMapButton = findViewById(R.id.viewMapButton);
        
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        
        // Set up button click listeners
        sendLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationAvailable) {
                    addPosition(latitude, longitude);
                } else {
                    // Try to get last known location
                    getLastKnownLocation();
                }
            }
        });
        
        viewMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                if (locationAvailable) {
                    intent.putExtra("latitude", latitude);
                    intent.putExtra("longitude", longitude);
                }
                startActivity(intent);
            }
        });

        // Check for permissions first
        checkAndRequestPermissions();
    }
    
    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastLocation != null) {
            latitude = lastLocation.getLatitude();
            longitude = lastLocation.getLongitude();
            altitude = lastLocation.getAltitude();
            accuracy = lastLocation.getAccuracy();
            
            updateLocationUI();
            locationAvailable = true;
            addPosition(latitude, longitude);
        } else {
            Toast.makeText(this, R.string.no_location, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateLocationUI() {
        latitudeTextView.setText("Latitude: " + String.format("%.6f", latitude));
        longitudeTextView.setText("Longitude: " + String.format("%.6f", longitude));
        altitudeTextView.setText("Altitude: " + String.format("%.2f", altitude) + " meters");
        accuracyTextView.setText("Accuracy: " + String.format("%.2f", accuracy) + " meters");
    }

    private void checkAndRequestPermissions() {
        String[] permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.INTERNET
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
                statusTextView.setText("Status: Permissions denied");
                sendLocationButton.setEnabled(false);
                viewMapButton.setEnabled(false);
            }
        }
    }

    private void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 150, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                altitude = location.getAltitude();
                accuracy = location.getAccuracy();
                locationAvailable = true;
                
                updateLocationUI();
                
                String msg = String.format(getResources().getString(R.string.new_location), latitude, longitude, altitude, accuracy);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                String newStatus = "";
                switch (status) {
                    case LocationProvider.OUT_OF_SERVICE:
                        newStatus = "OUT_OF_SERVICE";
                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        newStatus = "TEMPORARILY_UNAVAILABLE";
                        break;
                    case LocationProvider.AVAILABLE:
                        newStatus = "AVAILABLE";
                        break;
                }
                
                statusTextView.setText("Status: " + newStatus);
                String msg = String.format(getResources().getString(R.string.provider_new_status), provider, newStatus);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderEnabled(String provider) {
                statusTextView.setText("Status: Provider enabled");
                String msg = String.format(getResources().getString(R.string.provider_enabled), provider);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(String provider) {
                statusTextView.setText("Status: Provider disabled");
                String msg = String.format(getResources().getString(R.string.provider_disabled), provider);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Try to get initial location
        getLastKnownLocation();
    }

    void addPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(Request.Method.POST, insertUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Handle successful response
                        Toast.makeText(MainActivity.this, R.string.location_sent, Toast.LENGTH_SHORT).show();
                        statusTextView.setText("Status: Location sent successfully");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                        String errorMsg = getString(R.string.location_send_error, 
                                error.getMessage() != null ? error.getMessage() : "Unknown error");
                        Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        statusTextView.setText("Status: Error sending location");
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
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