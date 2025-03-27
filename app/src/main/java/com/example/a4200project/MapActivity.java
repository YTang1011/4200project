package com.example.a4200project;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private double currentLat, currentLng;
    private final List<Marker> allMarkers = new ArrayList<>();
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("ratings");
        currentUserId = getCurrentUserId();
        Log.d("MapActivity", "Current User ID: " + currentUserId);

        currentLat = getIntent().getDoubleExtra("lat", 0);
        currentLng = getIntent().getDoubleExtra("lng", 0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.getUid(); // Return UID for authenticated users
        }
        // For anonymous users, return a string like "anonymous"
        return "anonymous";
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        LatLng currentLocation = new LatLng(currentLat, currentLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

        String apiKey = getString(R.string.google_maps_key);

        // Fetch dedicated restrooms
        String mustHaveWashroomsUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + currentLat + "," + currentLng +
                "&radius=2000" +
                "&keyword=public%20restroom" +
                "&key=" + apiKey;
        fetchPlaces(mustHaveWashroomsUrl, BitmapDescriptorFactory.HUE_RED);

        // Fetch potential restrooms
        String possibleWashroomsUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + currentLat + "," + currentLng +
                "&radius=1000" +
                "&type=restaurant|cafe|shopping_mall|gas_station" +
                "&key=" + apiKey;
        fetchPlaces(possibleWashroomsUrl, BitmapDescriptorFactory.HUE_GREEN);

        // Marker click listener
        mMap.setOnMarkerClickListener(marker -> {
            hideAllMarkersExcept(marker);
            PlaceInfo info = (PlaceInfo) marker.getTag();
            if (info != null) {
                showBottomSheet(info);
            }
            return true;
        });

        // Map long click listener
        mMap.setOnMapLongClickListener(latLng -> showAllMarkers());
    }

    private void fetchPlaces(String urlString, float markerColor) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(stream);
                StringBuilder resultBuilder = new StringBuilder();
                int data;
                while ((data = reader.read()) != -1) {
                    resultBuilder.append((char) data);
                }
                String result = resultBuilder.toString();

                runOnUiThread(() -> {
                    try {
                        JSONObject object = new JSONObject(result);
                        JSONArray array = object.getJSONArray("results");

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject place = array.getJSONObject(i);
                            JSONObject location = place.getJSONObject("geometry")
                                    .getJSONObject("location");
                            String name = place.getString("name");
                            String address = place.optString("vicinity", "Address not available");
                            String placeId = place.getString("place_id");

                            LatLng latLng = new LatLng(location.getDouble("lat"),
                                    location.getDouble("lng"));

                            PlaceInfo placeInfo = new PlaceInfo(name, address, placeId);

                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
                            marker.setTag(placeInfo);

                            allMarkers.add(marker);

                            fetchPlaceDetails(placeId, placeInfo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showBottomSheet(PlaceInfo placeInfo) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_info, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Initialize views
        TextView tvName = bottomSheetView.findViewById(R.id.tvPlaceName);
        TextView tvAddress = bottomSheetView.findViewById(R.id.tvPlaceAddress);
        TextView tvHours = bottomSheetView.findViewById(R.id.tvPlaceHours);
        TextView tvFriendly = bottomSheetView.findViewById(R.id.tvFriendlyCount);
        TextView tvNotFriendly = bottomSheetView.findViewById(R.id.tvNotFriendlyCount);
        Button btnFriendly = bottomSheetView.findViewById(R.id.btnFriendly);
        Button btnNotFriendly = bottomSheetView.findViewById(R.id.btnNotFriendly);
        Button btnReset = bottomSheetView.findViewById(R.id.btnReset);

        // Set basic info
        tvName.setText(placeInfo.getName());
        tvAddress.setText(placeInfo.getAddress());

        // Set opening hours
        if (placeInfo.getOpeningHours() != null && !placeInfo.getOpeningHours().isEmpty()) {
            tvHours.setText(placeInfo.getOpeningHours());
        } else {
            tvHours.setText("Operating hours not available");
        }

        DatabaseReference placeRef = databaseReference.child(placeInfo.getPlaceId());
        DatabaseReference userVoteRef = placeRef.child("votes").child(currentUserId);
        DatabaseReference countsRef = placeRef.child("counts");

        // Real-time vote status listener
        userVoteRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    btnFriendly.setVisibility(View.GONE);
                    btnNotFriendly.setVisibility(View.GONE);
                    btnReset.setVisibility(View.VISIBLE);
                } else {
                    btnFriendly.setVisibility(View.VISIBLE);
                    btnNotFriendly.setVisibility(View.VISIBLE);
                    btnReset.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapActivity.this, "Error checking vote status", Toast.LENGTH_SHORT).show();
            }
        });

        // Real-time count updates
        countsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long friendly = snapshot.child("friendly").getValue(Long.class);
                Long notFriendly = snapshot.child("notFriendly").getValue(Long.class);

                tvFriendly.setText("Friendly: " + (friendly != null ? friendly : 0));
                tvNotFriendly.setText("Not Friendly: " + (notFriendly != null ? notFriendly : 0));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapActivity.this, "Failed to load ratings", Toast.LENGTH_SHORT).show();
            }
        });

        // Voting handlers
        btnFriendly.setOnClickListener(v -> castVote(placeRef, "friendly"));
        btnNotFriendly.setOnClickListener(v -> castVote(placeRef, "notFriendly"));
        btnReset.setOnClickListener(v -> resetVote(placeRef));

        bottomSheetDialog.show();
    }

    private void castVote(DatabaseReference placeRef, String voteType) {
        DatabaseReference userVoteRef = placeRef.child("votes").child(currentUserId);
        DatabaseReference countRef = placeRef.child("counts").child(voteType);

        // Check if user already voted
        userVoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Proceed with new vote
                    userVoteRef.setValue(voteType).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            countRef.runTransaction(new Transaction.Handler() {
                                @Override
                                public Transaction.Result doTransaction(MutableData mutableData) {
                                    Long current = mutableData.getValue(Long.class);
                                    mutableData.setValue((current != null ? current : 0L) + 1);
                                    return Transaction.success(mutableData);
                                }

                                @Override
                                public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                                    if (error != null) {
                                        Toast.makeText(MapActivity.this, "Vote failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapActivity.this, "Vote check failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetVote(DatabaseReference placeRef) {
        DatabaseReference userVoteRef = placeRef.child("votes").child(currentUserId);
        DatabaseReference countsRef = placeRef.child("counts");

        userVoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String previousVote = snapshot.getValue(String.class);
                    countsRef.child(previousVote).runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            Long current = mutableData.getValue(Long.class);
                            if (current != null && current > 0) {
                                mutableData.setValue(current - 1);
                            }
                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                            if (error == null) {
                                userVoteRef.removeValue().addOnSuccessListener(unused -> {
                                    // Successfully reset
                                });
                            } else {
                                Toast.makeText(MapActivity.this, "Reset failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapActivity.this, "Reset failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPlaceDetails(String placeId, PlaceInfo placeInfo) {
        new Thread(() -> {
            try {
                String detailsUrl = "https://maps.googleapis.com/maps/api/place/details/json" +
                        "?place_id=" + placeId +
                        "&fields=opening_hours" +
                        "&key=" + getString(R.string.google_maps_key);

                URL url = new URL(detailsUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                String result = new java.util.Scanner(stream).useDelimiter("\\A").next();

                JSONObject detailsObject = new JSONObject(result);
                JSONObject resultObject = detailsObject.getJSONObject("result");
                JSONObject openingHours = resultObject.optJSONObject("opening_hours");

                if (openingHours != null) {
                    JSONArray weekdayText = openingHours.optJSONArray("weekday_text");
                    if (weekdayText != null) {
                        StringBuilder hoursBuilder = new StringBuilder();
                        for (int i = 0; i < weekdayText.length(); i++) {
                            hoursBuilder.append(weekdayText.getString(i)).append("\n");
                        }
                        String hours = hoursBuilder.toString().trim();
                        runOnUiThread(() -> placeInfo.setOpeningHours(hours));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void hideAllMarkersExcept(Marker selectedMarker) {
        for (Marker marker : allMarkers) {
            if (!marker.equals(selectedMarker)) {
                marker.setVisible(false);
            }
        }
    }

    private void showAllMarkers() {
        for (Marker marker : allMarkers) {
            marker.setVisible(true);
        }
    }
}