package com.cfitzarl.townresolver;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;

import com.cfitzarl.townresolver.R;

/**
 * This is the sole activity for this app and thus represents all of its interactions.
 */
final class MainActivity extends AppCompatActivity {

    // This is an ambiguous integer representing the current activity
    private static final int REQUEST_PERMISSION_CODE = 10;

    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    /**
     * This is invoked when the activity is created and is responsible for
     * requesting appropriate permissions, setting the correct content, and
     * setting up listeners.
     *
     * @param savedInstanceState the state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSION_CODE);

        Button updateButton = (Button) findViewOrThrow(R.id.updateButton);

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchElements(View.INVISIBLE);
                updateCoordinates();
            }
        });

        updateCoordinates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    switchError(View.VISIBLE);
                } else {
                    switchError(View.INVISIBLE);
                }
            }
        }
    }

    /**
     * This will retrieve a location manager and request a location update.
     */
    private void updateCoordinates() {
        TextView locationText = (TextView) findViewOrThrow(R.id.locationText);

        try {
            LocationManager locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            LocationListenerImpl listener = new LocationListenerImpl(locationText);
            locMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
            locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        } catch (SecurityException e) {
            Log.e("GEO", "Security exception when getting location", e);
            switchError(View.VISIBLE);
        }
    }

    /**
     * This flips the loading panel on or off. This is called from an asynchronous context, so
     * thread synchronization prevents various "views" from going into an incorrect state.
     *
     * @param view the bit
     */
    private void switchElements(int view) {
        final TextView locationText = (TextView) findViewOrThrow(R.id.locationText);
        final View loadingView = findViewOrThrow(R.id.loadingPanel);
        final Button updateButton = (Button) findViewOrThrow(R.id.updateButton);

        synchronized (this) {
            switch (view) {
                case View.VISIBLE:
                    locationText.setVisibility(View.VISIBLE);
                    updateButton.setVisibility(View.VISIBLE);
                    loadingView.setVisibility(View.INVISIBLE);
                    break;
                case View.INVISIBLE:
                    locationText.setVisibility(View.INVISIBLE);
                    updateButton.setVisibility(View.INVISIBLE);
                    loadingView.setVisibility(View.VISIBLE);
                    break;
                case View.GONE:
                    locationText.setVisibility(View.INVISIBLE);
                    updateButton.setVisibility(View.INVISIBLE);
                    loadingView.setVisibility(View.INVISIBLE);
                    break;
                default:
                    throw new InvalidParameterException();
            }
        }
    }

    /**
     * This hides all location content and tells the user to grant location permissions. This
     * is called asynchronously, so thread synchronization has been introduced.
     */
    private synchronized void switchError(int view) {
        switchElements((view == View.INVISIBLE) ? View.INVISIBLE : View.GONE);
        TextView errorText = (TextView) findViewOrThrow(R.id.errorText);
        errorText.setVisibility(view);
    }

    /**
     * This wraps the ${@link #findViewById(int)} method and throws a ${@link NullPointerException}
     * if the view is not found. This prevents the need to write boilerplate null checks.
     *
     * @param viewId the id of the view
     * @return the view
     */
    private View findViewOrThrow(int viewId) {
        View view = findViewById(viewId);
        if (view == null) {
            throw new NullPointerException(String.format("Could not find view with id %s", viewId));
        }
        return view;
    }

    /**
     * This is an implementation of the ${@link LocationListener} interface that decodes
     * the current coordinates and updates a ${@link TextView} widget to contain the current
     * locality as well as the admin area, if it is present.
     */
    private class LocationListenerImpl implements LocationListener {

        private TextView locationText;

        LocationListenerImpl(TextView locationText) {
            this.locationText = locationText;
        }

        @Override
        public void onLocationChanged(Location location) {
            Geocoder geocoder = new Geocoder(getApplicationContext());
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                if (addresses != null) {
                    Address address = geocoder.getFromLocation(latitude, longitude, 1).get(0);
                    String adminArea = address.getAdminArea();
                    String locality = address.getLocality();
                    String text = "";

                    if (locality == null) {
                        text = "Unknown";
                    } else if (adminArea != null) {
                        text = String.format("%s, %s", address.getLocality(), adminArea);
                    } else {
                        text = locality;
                    }

                    locationText.setText(text);
                    switchElements(View.VISIBLE);
                }
            } catch (IOException e) {
                Log.e("GEO", "Error getting location", e);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onProviderDisabled(String provider) { }
    }
}
