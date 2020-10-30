package com.example.custom_gps;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.Marker;

import java.util.ArrayList;


public class GPSTracker extends Service implements LocationListener {

    private final Context mContext;

    Location location;

    double latitude;
    double longitude;
    double altitude;
    double speed;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    private static final long MIN_TIME_BW_UPDATES = 1000;
    protected LocationManager locationManager;

    public TextView androidLatitudeText;
    public TextView androidLongitudeText;
    public TextView androidSpeedText;

    public TextView revisedLatitudeText;
    public TextView revisedLongitudeText;
    public TextView revisedSpeedText;

    NaverMap mNaverMap;

    Marker androidMarker;
    Marker revisedMarker;

    ArrayList<Location> locations;

    public GPSTracker(Context context, TextView androidLatitudeText, TextView androidLongitudeText, TextView androidSpeedText, TextView revisedLatitudeText, TextView revisedLongitudeText, TextView revisedSpeedText) {
        this.mContext = context;

        this.androidLatitudeText = androidLatitudeText;
        this.androidLongitudeText = androidLongitudeText;
        this.androidSpeedText = androidSpeedText;

        this.revisedLatitudeText = revisedLatitudeText;
        this.revisedLongitudeText = revisedLongitudeText;
        this.revisedSpeedText = revisedSpeedText;

        androidMarker = new Marker();
        androidMarker.setHideCollidedMarkers(false);

        revisedMarker = new Marker();
        revisedMarker.setHideCollidedMarkers(false);

        locations = new ArrayList<Location>();

        getLocation();

        revision(location);
    }

    public void setNaverMap(NaverMap mNavermap){
        this.mNaverMap = mNavermap;
    }

    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                ;
            } else {
                int hasFineLocationPermission = ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION);

                if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                        hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
                    ;
                } else
                    return null;

//                if (isNetworkEnabled) {
//                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
//
//                    if (locationManager != null)
//                    {
//                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//                        if (location != null)
//                        {
//                            latitude = location.getLatitude();
//                            longitude = location.getLongitude();
//                            altitude = location.getAltitude();
//                        }
//                    }
//                }

                if (isGPSEnabled)
                {
                    if (location == null)
                    {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        if (locationManager != null)
                        {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null)
                            {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                altitude = location.getAltitude();
                                speed = location.getSpeed();
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Log.d("GPSTracker", e.toString());
        }

        androidLatitudeText.setText("위도: " + latitude);
        androidLongitudeText.setText("경도: " + longitude);
        androidSpeedText.setText("속도: " + speed);

        androidMarker.setPosition(new com.naver.maps.geometry.LatLng(latitude, longitude));
        androidMarker.setIconTintColor(Color.YELLOW);
        androidMarker.setCaptionText("내장");
        androidMarker.setMap(mNaverMap);

        return location;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        altitude = location.getAltitude();
        speed = location.getSpeed();

        androidLatitudeText.setText("위도: " + latitude);
        androidLongitudeText.setText("경도: " + longitude);
        androidSpeedText.setText("속도: " + speed);

        androidMarker.setPosition(new com.naver.maps.geometry.LatLng(latitude, longitude));
        androidMarker.setIconTintColor(Color.YELLOW);
        androidMarker.setCaptionText("내장");
        androidMarker.setMap(mNaverMap);

        revision(location);
    }

    @Override
    public void onProviderDisabled(String provider)
    {
    }

    @Override
    public void onProviderEnabled(String provider)
    {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    public void revision(Location location)
    {
        double alpha;

        if (locations.size() < 7)
        {
            locations.add(location);

            revisedLatitudeText.setText("위도: " + latitude);
            revisedLongitudeText.setText("경도: " + longitude);
            revisedSpeedText.setText("속도: " + speed);

            revisedMarker.setPosition(new com.naver.maps.geometry.LatLng(latitude, longitude));
            revisedMarker.setIconTintColor(Color.RED);
            revisedMarker.setCaptionText("보정");
            revisedMarker.setMap(mNaverMap);
        }
        else
        {
            int index = locations.size() - 7;

            Location prevLOC = locations.get(index+6);
            Location currLOC = location;

            double prevSpeed = prevLOC.getSpeed();
            double currSpeed = currLOC.getSpeed();

            double v1 = (locations.get(index).getSpeed() + locations.get(index+1).getSpeed() + locations.get(index+2).getSpeed()) / 3;
            double v2 = (locations.get(index+2).getSpeed() + locations.get(index+3).getSpeed() + locations.get(index+4).getSpeed()) / 3;
            double v3 = (locations.get(index+4).getSpeed() + locations.get(index+5).getSpeed() + locations.get(index+6).getSpeed()) / 3;

            double a1 = v2 - v1;
            double a2 = v3 - v2;

            if (prevSpeed < 1)
                alpha = 0.7f;
            else if (prevSpeed < 2)
                alpha = 0.75f;
            else if (prevSpeed < 5)
                alpha = 0.9f;
            else
                alpha = 0.95f;

            double revisedSpeed = alpha * currSpeed + (1-alpha) * (prevSpeed + (a2-a1));

            if (revisedSpeed < 0)
                revisedSpeed = 0;

            double[] prevXYZ = CoordinateConversions.getXYZfromLatLonDegrees(locations.get(index+6).getLatitude(), locations.get(index+6).getLongitude(), locations.get(index+6).getAltitude());
            double[] currXYZ = CoordinateConversions.getXYZfromLatLonDegrees(currLOC.getLatitude(), currLOC.getLongitude(), currLOC.getAltitude());

            currXYZ[0] = prevXYZ[0] + (currXYZ[0] - prevXYZ[0]) * (revisedSpeed / currSpeed);
            currXYZ[1] = prevXYZ[1] + (currXYZ[1] - prevXYZ[1]) * (revisedSpeed / currSpeed);
            currXYZ[1] = prevXYZ[1] + (currXYZ[1] - prevXYZ[1]) * (revisedSpeed / currSpeed);

            double[] result = CoordinateConversions.xyzToLatLonDegrees(currXYZ);

            double revisedLatitude = result[0];
            double revisedLongitude = result[1];
            double revisedAltitude = result[2];

            location.setLatitude(revisedLatitude);
            location.setLongitude(revisedLongitude);
            location.setAltitude(revisedAltitude);
            location.setSpeed((float) revisedSpeed);

            locations.add(location);

            revisedLatitudeText.setText("위도: " + revisedLatitude);
            revisedLongitudeText.setText("경도: " + revisedLongitude);
            revisedSpeedText.setText("속도: " + revisedSpeed);

            revisedMarker.setPosition(new com.naver.maps.geometry.LatLng(revisedLatitude, revisedLongitude));
            revisedMarker.setIconTintColor(Color.BLUE);
            revisedMarker.setCaptionText("보정");
            revisedMarker.setMap(mNaverMap);
        }
    }
}
