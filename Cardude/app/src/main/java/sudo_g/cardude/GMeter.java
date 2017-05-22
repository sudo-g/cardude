package sudo_g.cardude;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import sudo_g.cardude.OrientationManager.DeviceOrientation;

public class GMeter
{
    private static final int MAX_ACC_MS2 = 10;
    private static final int GUI_UPDATE_INTERVAL_MS = 30;

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

    private Context mContext;
    private final Handler mGuiUpdater = new Handler();
    private final Runnable mGuiTask = new Runnable()
    {
        @Override
        public void run()
        {
            mGuiUpdater.postDelayed(mGuiTask, GUI_UPDATE_INTERVAL_MS);
            if (mGuiElement != null)
            {
                float localAccX = correctForRotation(mLocalAcc, mRotationAngle)[0];
                final int barCenterOffset = mGuiElement.getMax() / 2;
                int seekBarProgress = (int) -localAccX * mGuiElement.getMax() / MAX_ACC_MS2 + barCenterOffset;
                mGuiElement.setProgress(seekBarProgress);
            }
        }
    };

    public GMeter(Context context)
    {
        mContext = context;
    }

    public void start()
    {

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(mSensorCallbacks, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        // start the task that periodically updates the indicator
        if (mGuiElement != null)
        {
            mGuiElement.setProgress(mGuiElement.getMax() / 2);
        }
        mGuiUpdater.postDelayed(mGuiTask, GUI_UPDATE_INTERVAL_MS);
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
    public void setRotationAngle(DeviceOrientation rotation)
    {
        switch (rotation)
        {
            case PORTRAIT:
                mRotationAngle = 0;
                break;
            case LANDSCAPE:
                mRotationAngle = 90;
                break;
            case REVERSE_PORTRAIT:
                mRotationAngle = 180;
                break;
            case REVERSE_LANDSCAPE:
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
