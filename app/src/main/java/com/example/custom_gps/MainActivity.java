package com.example.custom_gps;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, com.google.android.gms.maps.OnMapReadyCallback {
    TextView androidLatitude;
    TextView androidLongitude;
    TextView androidSpeed;

    TextView revisedLatitude;
    TextView revisedLongitude;
    TextView revisedSpeed;

    // 안드로이드 내장 GPS
    private GPSTracker gpsTracker;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    Marker androidMarker;

    Marker revisedMarker;

    // 네이버 지도 API
    private NaverMap mNaverMap;
    private FusedLocationSource mLocationSource;
    Marker naverMarker;

//    TextView naverLatitude;
//    TextView naverLongitude;

    // 구글 지도 API
    private GoogleMap mGoogleMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private Location location;
    private static final int UPDATE_INTERVAL_MS = 1000; // 1초
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5초
    private com.google.android.gms.maps.model.Marker currentMarker = null;
    Marker googleMarker;

//    TextView googleLatitude;
//    TextView googleLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        androidLatitude = findViewById(R.id.androidLatitude);
        androidLongitude = findViewById(R.id.androidLongitude);
        androidSpeed = findViewById(R.id.androidSpeed);

//        naverLatitude = findViewById(R.id.naverLatitude);
//        naverLongitude = findViewById(R.id.naverLongitude);
//
//        googleLatitude = findViewById(R.id.googleLatitude);
//        googleLongitude = findViewById(R.id.googleLongitude);

        revisedLatitude = findViewById(R.id.revisedLatitude);
        revisedLongitude = findViewById(R.id.revisedLongitude);
        revisedSpeed = findViewById(R.id.revisedSpeed);

        // 안드로이드 내장 Location 함수
        androidMarker = new Marker();
        androidMarker.setHideCollidedMarkers(false);
        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }

        gpsTracker = new GPSTracker(com.example.custom_gps.MainActivity.this, androidLatitude, androidLongitude, androidSpeed, revisedLatitude, revisedLongitude, revisedSpeed);

        // 네이버 지도 API
        naverMarker = new Marker();
        naverMarker.setHideCollidedMarkers(false);

        FragmentManager fm = getSupportFragmentManager();
        MapFragment navermapFragment = (MapFragment) fm.findFragmentById(R.id.naverMap);
        if (navermapFragment == null) {
            navermapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.naverMap, navermapFragment).commit();
        }

        navermapFragment.getMapAsync(this);

        mLocationSource =
                new FusedLocationSource(this, PERMISSIONS_REQUEST_CODE);

        // 구글 지도 API
        googleMarker = new Marker();
        googleMarker.setHideCollidedMarkers(false);

        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment googlemapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.googleMap);
        googlemapFragment.getMapAsync(this);
    }


    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {
        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults);
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {
            boolean check_result = true;

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if (check_result) {
                ;
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {
                    Toast.makeText(com.example.custom_gps.MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(com.example.custom_gps.MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission(){
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(com.example.custom_gps.MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(com.example.custom_gps.MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            ;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(com.example.custom_gps.MainActivity.this, REQUIRED_PERMISSIONS[0])) {
                Toast.makeText(com.example.custom_gps.MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(com.example.custom_gps.MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);

            } else {
                ActivityCompat.requestPermissions(com.example.custom_gps.MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
    }


    public String getCurrentAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";
        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString()+"\n";
    }


    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(com.example.custom_gps.MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("MainActivity", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        mNaverMap = naverMap;

        mNaverMap.setLocationSource(mLocationSource);

        LocationOverlay locationOverlay = mNaverMap.getLocationOverlay();
        locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.position));
        locationOverlay.setVisible(false);
        locationOverlay.setIconWidth(0);
        locationOverlay.setIconHeight(0);

        gpsTracker.setNaverMap(mNaverMap);

        mNaverMap.addOnLocationChangeListener(new NaverMap.OnLocationChangeListener() {
            @Override
            public void onLocationChange(@NonNull Location location) {
//                Toast.makeText(MainActivity.this, "addOnLocationChangeListener OK", Toast.LENGTH_SHORT).show();
//                naverLatitude.setText("위도: " + location.getLatitude());
//                naverLongitude.setText("경도: " + location.getLongitude());

//                naverMarker.setPosition(new com.naver.maps.geometry.LatLng(location.getLatitude(), location.getLongitude()));
//                naverMarker.setIconTintColor(Color.RED);
//                naverMarker.setCaptionText("네이버");
//                naverMarker.setMap(mNaverMap);
            }
        });

        mNaverMap.addOnCameraIdleListener(new NaverMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                if (mNaverMap.getLocationTrackingMode() == LocationTrackingMode.NoFollow ||
                        mNaverMap.getLocationTrackingMode() == LocationTrackingMode.None) {
                    mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);
        mGoogleMap.moveCamera(cameraUpdate);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED   ) {
            startLocationUpdates();
        }

        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();

            if (locationList.size() > 0) {
                location = locationList.get(locationList.size() - 1);

//                googleLatitude.setText("위도: " + location.getLatitude());
//                googleLongitude.setText("경도: " + location.getLongitude());

//                googleMarker.setPosition(new com.naver.maps.geometry.LatLng(location.getLatitude(), location.getLongitude()));
//                googleMarker.setIconTintColor(Color.GREEN);
//                googleMarker.setCaptionText("구글");
//                googleMarker.setMap(mNaverMap);
            }
        }
    };

    private void startLocationUpdates() {
        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        } else {
            int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);

            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

            if (checkPermission())
                mGoogleMap.setMyLocationEnabled(true);
        }
    }

    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {
        if (currentMarker != null) currentMarker.remove();

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);

        currentMarker = mGoogleMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
        mGoogleMap.moveCamera(cameraUpdate);
    }

    private boolean checkPermission() {
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        return false;
    }
}