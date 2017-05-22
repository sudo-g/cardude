package sudo_g.cardude;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Speedometer extends LinearLayout implements LocationService.SpeedSubscriber
{
    private static final int GUI_UPDATE_INTERVAL_MS = 30;

    private LocationService mLocationManager;
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

    public Speedometer(Context context)
    {
        super(context);
        initializeViews();
    }

    public Speedometer(Context context, AttributeSet attrs)
    {
        super(context, attrs, 0);
        initializeViews();
    }

    public Speedometer(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initializeViews();
    }

    public void start(LocationService locationService)
    {
        mLocationManager = locationService;
        mLocationManager.addSpeedSubscription(this);
        if (mSpeedIndicator != null)
        {
            mSpeedIndicator.postDelayed(mGuiTask, GUI_UPDATE_INTERVAL_MS);
        }
    }

    public void stop()
    {
        mLocationManager.removeSpeedSubscription(this);
    }

    public void updateSpeed(Float speed)
    {
        mSpeedMs = speed;
    }

    private void initializeViews()
    {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View layout = inflater.inflate(R.layout.speedometer, this);

        mSpeedIndicator = (TextView) layout.findViewById(R.id.speed_text);
        mUnitIndicator = (TextView) layout.findViewById(R.id.unit_text);
    }

    private float ms2kmh(float speedMs)
    {
        return (float) speedMs * 3.6F;
    }
}
