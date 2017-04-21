package sudo_g.cardude;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.SeekBar;

public class GMeter
{
    static final int MAX_ACC_MS2 = 10;

    private SeekBar mGuiElement;

    private volatile float[] mLocalAcc = {0, 0, 0};
    private int mRotationAngle = 0;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SensorEventListener mSensorCallbacks = new SensorEventListener()
    {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            Sensor sensorChanged = sensorEvent.sensor;
            if (sensorChanged.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
            {
                mLocalAcc = sensorEvent.values;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i)
        {

        }
    };

    private Activity mHostActivity;
    private final Handler mGuiUpdater = new Handler();
    private final Runnable mGuiTask = new Runnable()
    {
        @Override
        public void run()
        {
            mGuiUpdater.postDelayed(mGuiTask, 50);
            if (mGuiElement != null)
            {
                float localAccX = correctForRotation(mLocalAcc, mRotationAngle)[0];
                int seekBarProgress = (int) -localAccX * mGuiElement.getMax() / MAX_ACC_MS2 + 50;
                mGuiElement.setProgress(seekBarProgress);
            }
        }
    };

    public GMeter(Activity host)
    {
        mHostActivity = host;
    }

    public void start()
    {

        mSensorManager = (SensorManager) mHostActivity.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(mSensorCallbacks, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        // start the task that periodically updates the indicator
        mGuiUpdater.postDelayed(mGuiTask, 50);
    }

    public void stop()
    {
        mSensorManager.unregisterListener(mSensorCallbacks);
    }

    /**
     * Bind layout object to this g-meter instance.
     *
     * @param guiElement Element objects to bind this instance to.
     */
    public void bindGuiElement(SeekBar guiElement)
    {
        mGuiElement = guiElement;

        // prevent touch events from changing seek bar position.
        mGuiElement.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
    }

    /**
     * Inform the g-meter device has rotated so indicator uses the correct accelerometer axis.
     *
     * @param rotation Description of the rotation angle.
     */
    public void setRotationAngle(int rotation)
    {
        switch (rotation)
        {
            case Surface.ROTATION_0:
                mRotationAngle = 0;
                break;
            case Surface.ROTATION_90:
                mRotationAngle = 90;
                break;
            case Surface.ROTATION_180:
                mRotationAngle = 180;
                break;
            case Surface.ROTATION_270:
                mRotationAngle = 270;
                break;
        }
    }

    private float[] correctForRotation(float[] localAcc, int rotation)
    {
        double theta = Math.toRadians((double) rotation);
        double cs = Math.cos(theta);
        double sn = Math.sin(theta);

        float[] globalAcc =
        {
            (float) (localAcc[0] * cs - localAcc[1] * sn),
            (float) (localAcc[0] * sn - localAcc[1] * cs),
            localAcc[2]
        };

        return globalAcc;
    }
}
