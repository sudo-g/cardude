package sudo_g.cardude;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;

public class OrientationManager
{
    private static final int ROTATION_THRESHOLD = 15;
    private static final int PORTRAIT_ANGLE = 360;
    private static final int REVERSE_PORTRAIT_ANGLE = PORTRAIT_ANGLE / 2;
    private static final int LANDSCAPE_ANGLE = PORTRAIT_ANGLE / 4;
    private static final int REVERSE_LANDSCAPE_ANGLE = REVERSE_PORTRAIT_ANGLE + LANDSCAPE_ANGLE;

    enum DeviceOrientation
    {
        PORTRAIT, REVERSE_PORTRAIT, REVERSE_LANDSCAPE, LANDSCAPE
    }

    interface Listener
    {
        public void onOrientationChanged(DeviceOrientation orientation);
    }

    private DeviceOrientation mCurrentOrientation = DeviceOrientation.PORTRAIT;
    private Listener mListener;
    private Activity mHostActivity;
    private OrientationEventListener mOrientationEventListener;

    public OrientationManager(Activity activity, Listener listener)
    {
        mHostActivity = activity;
        mListener = listener;
        mOrientationEventListener = new OrientationEventListener(mHostActivity, SensorManager.SENSOR_DELAY_NORMAL)
        {
            @Override
            public void onOrientationChanged(int rotation)
            {
                if ((rotation <= ROTATION_THRESHOLD || rotation > (PORTRAIT_ANGLE - ROTATION_THRESHOLD))
                        && mCurrentOrientation != DeviceOrientation.PORTRAIT)
                {
                    mCurrentOrientation = DeviceOrientation.PORTRAIT;
                    mHostActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    invokeListenerIfNotNull(DeviceOrientation.PORTRAIT);
                }
                else if (rotation >= (REVERSE_PORTRAIT_ANGLE -ROTATION_THRESHOLD)
                        && rotation < (REVERSE_PORTRAIT_ANGLE +ROTATION_THRESHOLD)
                        && mCurrentOrientation != DeviceOrientation.REVERSE_PORTRAIT)
                {
                    mCurrentOrientation = DeviceOrientation.REVERSE_PORTRAIT;
                    mHostActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    invokeListenerIfNotNull(DeviceOrientation.REVERSE_PORTRAIT);
                }
                else if (rotation >= (LANDSCAPE_ANGLE-ROTATION_THRESHOLD)
                        && rotation < (LANDSCAPE_ANGLE+ROTATION_THRESHOLD)
                        && mCurrentOrientation != DeviceOrientation.REVERSE_LANDSCAPE)
                {
                    mCurrentOrientation = DeviceOrientation.REVERSE_LANDSCAPE;
                    mHostActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    invokeListenerIfNotNull(DeviceOrientation.REVERSE_LANDSCAPE);
                }
                else if (rotation >= (REVERSE_LANDSCAPE_ANGLE-ROTATION_THRESHOLD)
                        && rotation < (REVERSE_LANDSCAPE_ANGLE+ROTATION_THRESHOLD)
                        && mCurrentOrientation != DeviceOrientation.LANDSCAPE)
                {
                    mCurrentOrientation = DeviceOrientation.LANDSCAPE;
                    mHostActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    invokeListenerIfNotNull(DeviceOrientation.LANDSCAPE);
                }
            }
        };
    }

    public void start()
    {
        mOrientationEventListener.enable();
    }

    public void stop()
    {
        mOrientationEventListener.disable();
    }

    private void invokeListenerIfNotNull(DeviceOrientation orientation)
    {
        if (mListener != null)
        {
            mListener.onOrientationChanged(orientation);
        }
    }
}
