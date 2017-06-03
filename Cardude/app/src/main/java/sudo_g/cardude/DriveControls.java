package sudo_g.cardude;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

public class DriveControls extends RelativeLayout
{
    public interface Listener
    {
        /**
         * Called when a request to take a picture is received.
         */
        public void takePictureRequest();

        /**
         * Called when a drive is started.
         */
        public void onDriveStart();

        /**
         * Called when a request to record the last few seconds of captured video is requested.
         */
        public void captureLastVideoBufferRequest();
    }

    private Listener mListener;

    private LocationService mLocationService;

    private Button mSnapshotButton;
    private Button mVideoButton;
    private Button mDriveButton;

    private GMeter mGMeter;
    private Speedometer mSpeedometer;

    public DriveControls(Context context)
    {
        super(context);
        initializeViews();
    }

    public DriveControls(Context context, AttributeSet attrs)
    {
        super(context, attrs, 0);
        initializeViews();
    }

    public DriveControls(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initializeViews();
    }

    public void start(LocationService locationService)
    {
        mLocationService = locationService;

        mGMeter.start();
        mSpeedometer.start(mLocationService);
    }

    public void stop()
    {
        mGMeter.stop();
        mSpeedometer.stop();
    }

    public void setRotationAngle(OrientationManager.DeviceOrientation rotation)
    {
        if (mGMeter != null)
        {
            mGMeter.setRotationAngle(rotation);
        }
    }

    public void setEventListener(Listener listener)
    {
        mListener = listener;
    }

    private void initializeViews()
    {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View layout = inflater.inflate(R.layout.cam_overlays, this);

        mGMeter = (GMeter) layout.findViewById(R.id.gmeter);

        mSpeedometer = (Speedometer) layout.findViewById(R.id.speedometer);

        mDriveButton = (Button) layout.findViewById(R.id.start_button);
        mDriveButton.setOnClickListener(
            new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    if (mListener != null)
                    {
                        mListener.onDriveStart();
                    }
                }
            }
        );

        mSnapshotButton = (Button) layout.findViewById(R.id.snapshotbutton);
        mSnapshotButton.setOnClickListener(
            new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    if (mListener != null)
                    {
                        mListener.takePictureRequest();
                    }
                }
            }
        );

        mVideoButton = (Button) layout.findViewById(R.id.video_button);
        mVideoButton.setOnClickListener(
            new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    if (mListener != null)
                    {
                        mListener.captureLastVideoBufferRequest();
                    }
                }
            }
        );
    }
}
