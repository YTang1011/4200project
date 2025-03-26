package com.example.a4200project;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private double currentLat, currentLng;
    private final List<Marker> allMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        currentLat = getIntent().getDoubleExtra("lat", 0);
        currentLng = getIntent().getDoubleExtra("lng", 0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
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

        String mustHaveWashroomsUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + currentLat + "," + currentLng +
                "&radius=2000" +
                "&keyword=public%20restroom" +
                "&key=" + apiKey;
        fetchPlaces(mustHaveWashroomsUrl, BitmapDescriptorFactory.HUE_RED);

        String possibleWashroomsUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + currentLat + "," + currentLng +
                "&radius=1000" +
                "&type=restaurant|cafe|shopping_mall|gas_station" +
                "&key=" + apiKey;
        fetchPlaces(possibleWashroomsUrl, BitmapDescriptorFactory.HUE_GREEN);

        mMap.setOnMarkerClickListener(marker -> {
            hideAllMarkersExcept(marker);
            PlaceInfo info = (PlaceInfo) marker.getTag();
            if (info != null) {
                showBottomSheet(info);
            }
            return true;
        });

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

                            PlaceInfo placeInfo = new PlaceInfo(name, address);

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

    private void showBottomSheet(PlaceInfo placeInfo) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_info, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        TextView tvName = bottomSheetView.findViewById(R.id.tvPlaceName);
        TextView tvAddress = bottomSheetView.findViewById(R.id.tvPlaceAddress);
        TextView tvHours = bottomSheetView.findViewById(R.id.tvPlaceHours);

        tvName.setText(placeInfo.getName());
        tvAddress.setText(placeInfo.getAddress());

        if (placeInfo.getOpeningHours() != null && !placeInfo.getOpeningHours().isEmpty()) {
            tvHours.setText(placeInfo.getOpeningHours());
        } else {
            tvHours.setText("Operating hours not available");
        }

        bottomSheetDialog.show();
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