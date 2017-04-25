package sudo_g.cardude;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.OrientationEventListener;

public class OrientationManager
{
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
        mOrientationEventListener = new OrientationEventListener(mHostActivity)
        {
            @Override
            public void onOrientationChanged(int rotation)
            {
                if( (rotation <= 45 || rotation > 315) && mCurrentOrientation != DeviceOrientation.PORTRAIT)
                {
                    mCurrentOrientation = DeviceOrientation.PORTRAIT;
                    mHostActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    invokeListenerIfNotNull(DeviceOrientation.PORTRAIT);
                }
                else if (rotation >= 135 && rotation < 225 && mCurrentOrientation != DeviceOrientation.REVERSE_PORTRAIT)
                {
                    mCurrentOrientation = DeviceOrientation.REVERSE_PORTRAIT;
                    mHostActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    invokeListenerIfNotNull(DeviceOrientation.REVERSE_PORTRAIT);
                }
                else if (rotation >= 45 && rotation < 135 && mCurrentOrientation != DeviceOrientation.REVERSE_LANDSCAPE)
                {
                    mCurrentOrientation = DeviceOrientation.REVERSE_LANDSCAPE;
                    mHostActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    invokeListenerIfNotNull(DeviceOrientation.REVERSE_LANDSCAPE);
                }
                else if (rotation >= 225 && rotation < 320 && mCurrentOrientation != DeviceOrientation.LANDSCAPE)
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
