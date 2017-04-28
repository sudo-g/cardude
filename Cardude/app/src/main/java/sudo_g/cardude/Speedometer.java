package sudo_g.cardude;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Speedometer extends LinearLayout
{
    private static final int FIX_TIMEOUT_MS = 1000 * 60 * 2;
    private static final int GUI_UPDATE_INTERVAL_MS = 30;

    private LocationManager mLocationManager;
    private TextView mSpeedIndicator;
    private TextView mUnitIndicator;
    private Float mSpeedMs = null;

    private final Handler mGuiUpdater = new Handler();
    private final Runnable mGuiTask = new Runnable()
    {
        @Override
        public void run()
        {
            mGuiUpdater.postDelayed(mGuiTask, GUI_UPDATE_INTERVAL_MS);
            if (mSpeedIndicator != null)
            {
                String speedText = (mSpeedMs == null) ? "--" : String.format("%.0f", ms2kmh(mSpeedMs));
                mSpeedIndicator.setText(speedText);
            }
        }
    };


    private LocationListener mLocationListener = new LocationListener()
    {
        public void onLocationChanged(Location location)
        {
            if (location.hasSpeed())
            {
                mSpeedMs = location.getSpeed();
            }
            else
            {
                mSpeedMs = null;
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

        public void onProviderEnabled(String provider)
        {

        }

        public void onProviderDisabled(String provider)
        {

        }
    };

    public Speedometer(Context context)
    {
        super(context);
        initializeViews(context);
    }

    public Speedometer(Context context, AttributeSet attrs)
    {
        super(context, attrs, 0);
        initializeViews(context);
    }

    public Speedometer(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initializeViews(context);
    }

    public void start()
    {
        mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);

        if (mSpeedIndicator != null)
        {
            mSpeedIndicator.postDelayed(mGuiTask, GUI_UPDATE_INTERVAL_MS);
        }
    }

    public void stop()
    {
        mLocationManager.removeUpdates(mLocationListener);
    }

    private void initializeViews(Context context)
    {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View layout = inflater.inflate(R.layout.speedometer, this);

        mSpeedIndicator = (TextView) layout.findViewById(R.id.speed_text);
        mUnitIndicator = (TextView) layout.findViewById(R.id.unit_text);
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    private boolean isBetterLocation(Location location, Location currentBestLocation)
    {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > FIX_TIMEOUT_MS;
        boolean isSignificantlyOlder = timeDelta < -FIX_TIMEOUT_MS;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer)
        {
            return true;
            // If the new location is more than two minutes older, it must be worse
        }
        else if (isSignificantlyOlder)
        {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate)
        {
            return true;
        }
        else if (isNewer && !isLessAccurate)
        {
            return true;
        }
        else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider)
        {
            return true;
        }
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2)
    {
        if (provider1 == null)
        {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private float ms2kmh(float speedMs)
    {
        return (float) speedMs * 3.6F;
    }
}
