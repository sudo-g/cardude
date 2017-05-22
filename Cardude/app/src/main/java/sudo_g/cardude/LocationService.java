package sudo_g.cardude;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class LocationService
{
    // constants
    private static final int FIX_TIMEOUT_MS = 1000 * 60 * 2;

    // subscription interfaces
    public interface SpeedSubscriber
    {
        public void updateSpeed(Float speed);
    }

    public interface LocationSubscriber
    {
        public void updateLocation(double latitude, double longitude);
    }

    private static LocationService singletonLocationService;

    private LocationManager mLocationManager;
    private final Context mContext;

    private List<LocationSubscriber> mLocationSubscribers = new ArrayList<LocationSubscriber>();
    private List<SpeedSubscriber> mSpeedSubscribers = new ArrayList<SpeedSubscriber>();

    private LocationListener mLocationListener = new LocationListener()
    {
        public void onLocationChanged(Location location)
        {
            for (LocationSubscriber subscriber : mLocationSubscribers)
            {
                subscriber.updateLocation(location.getLatitude(), location.getLongitude());
            }

            if (location.hasSpeed())
            {
                for (SpeedSubscriber subscriber : mSpeedSubscribers)
                {
                    subscriber.updateSpeed(location.getSpeed());
                }
            }
            else
            {
                for (SpeedSubscriber subscriber : mSpeedSubscribers)
                {
                    subscriber.updateSpeed(null);
                }
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

    /**
     * Gets a handle to the location service.
     *
     * @param context Application context.
     * @return Handle to the service.
     */
    public static LocationService getLocationService(Context context)
    {
        if (singletonLocationService == null)
        {
            singletonLocationService = new LocationService(context);
        }
        return singletonLocationService;
    }

    private LocationService(Context context)
    {
        mContext = context;
    }

    public void start()
    {
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    }

    public void stop()
    {
        mLocationManager.removeUpdates(mLocationListener);
    }

    public void addSpeedSubscription(SpeedSubscriber subscriber)
    {
        mSpeedSubscribers.add(subscriber);
    }

    public void removeSpeedSubscription(SpeedSubscriber subscriber)
    {
        mSpeedSubscribers.remove(subscriber);
    }

    public void addLocationSubscription(LocationSubscriber subscriber)
    {
        mLocationSubscribers.add(subscriber);
    }

    public void removeLocationSubscription(LocationSubscriber subscriber)
    {
        mLocationSubscribers.remove(subscriber);
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
}
