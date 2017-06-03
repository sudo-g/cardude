package sudo_g.cardude;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import sudo_g.cardude.OrientationManager.DeviceOrientation;

public class GMeter extends RelativeLayout
{
    private static final int MAX_ACC_MS2 = 10;
    private static final int GUI_UPDATE_INTERVAL_MS = 30;
    private static final float MS2_PER_G = 9.81F;

    private SeekBar mAnalogIndicator;
    private TextView mDigitalIndicator;
    private TextView[] mTickMarks = new TextView[4];
    private TextView[] mNumMarks = new TextView[5];

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
            if (mAnalogIndicator != null)
            {
                float localAccX = correctForRotation(mLocalAcc, mRotationAngle)[0];

                final int barCenterOffset = mAnalogIndicator.getMax() / 2;
                int seekBarProgress = (int) -localAccX * mAnalogIndicator.getMax() / MAX_ACC_MS2 + barCenterOffset;
                mAnalogIndicator.setProgress(seekBarProgress);

                float digitalValue = localAccX / MS2_PER_G;
                if (Math.abs(digitalValue) < 0.05)
                {
                    // prevent oscillating sign change near 0
                    digitalValue = 0;
                }
                mDigitalIndicator.setText(String.format("%.1f", digitalValue));
            }
        }
    };

    public GMeter(Context context)
    {
        super(context);
        mContext = context;
        initializeViews();
    }

    public GMeter(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
        initializeViews();
    }

    public GMeter(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        mContext = context;
        initializeViews();
    }

    public void start()
    {

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(mSensorCallbacks, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        // start the task that periodically updates the indicator
        if (mAnalogIndicator != null)
        {
            mAnalogIndicator.setProgress(mAnalogIndicator.getMax() / 2);
        }
        mGuiUpdater.postDelayed(mGuiTask, GUI_UPDATE_INTERVAL_MS);
    }

    public void stop()
    {
        mSensorManager.unregisterListener(mSensorCallbacks);
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

    private void initializeViews()
    {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View layout = inflater.inflate(R.layout.gmeter, this);

        mAnalogIndicator = (SeekBar) layout.findViewById(R.id.g_analog_indicator);
        // prevent touch events from changing seek bar position.
        mAnalogIndicator.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        mDigitalIndicator = (TextView) layout.findViewById(R.id.g_digital);

        mTickMarks[0] = (TextView) layout.findViewById(R.id.g_tick_left);
        mTickMarks[1] = (TextView) layout.findViewById(R.id.g_tick_midleft);
        mTickMarks[2] = (TextView) layout.findViewById(R.id.g_tick_midright);
        mTickMarks[3] = (TextView) layout.findViewById(R.id.g_tick_right);

        mNumMarks[0] = (TextView) layout.findViewById(R.id.g_num_left);
        mNumMarks[1] = (TextView) layout.findViewById(R.id.g_num_midleft);
        mNumMarks[2] = (TextView) layout.findViewById(R.id.g_num_center);
        mNumMarks[3] = (TextView) layout.findViewById(R.id.g_num_midright);
        mNumMarks[4] = (TextView) layout.findViewById(R.id.g_num_right);
    }
}
