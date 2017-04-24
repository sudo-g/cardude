package sudo_g.cardude;

import android.graphics.PixelFormat;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class CameraActivity extends ActionBarActivity {

    LayoutInflater controlInflater = null;

    private int mCurrentOrientation = Surface.ROTATION_0;
    private OrientationEventListener mOrientationEventListener;

    private CameraSurface mCameraSurface;
    private Button mSnapshotButton;
    private final GMeter mGMeter = new GMeter(this);
    private final Speedometer mSpeedometer = new Speedometer(this);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().setFormat(PixelFormat.UNKNOWN);

        mOrientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL)
        {
            @Override
            public void onOrientationChanged(int rotation)
            {
                if( (rotation < 35 || rotation > 325) && mCurrentOrientation != Surface.ROTATION_0)
                {
                    // PORTRAIT
                    mCurrentOrientation = Surface.ROTATION_0;
                    mGMeter.setRotationAngle(mCurrentOrientation);
                }
                else if (rotation > 145 && rotation < 215 && mCurrentOrientation != Surface.ROTATION_180)
                {
                    // REVERSE PORTRAIT
                    mCurrentOrientation = Surface.ROTATION_180;
                    mGMeter.setRotationAngle(mCurrentOrientation);
                }
                else if (rotation > 55 && rotation < 125 && mCurrentOrientation != Surface.ROTATION_270)
                {
                    // REVERSE LANDSCAPE
                    mCurrentOrientation = Surface.ROTATION_270;
                    mGMeter.setRotationAngle(mCurrentOrientation);
                }
                else if (rotation > 235 && rotation < 305 && mCurrentOrientation != Surface.ROTATION_90)
                {
                    //LANDSCAPE
                    mCurrentOrientation = Surface.ROTATION_90;
                    mGMeter.setRotationAngle(mCurrentOrientation);
                }
            }
        };

        // start camera
        mCameraSurface = (CameraSurface) findViewById(R.id.camerapreview);
        mCameraSurface.start();

        // add overlays
        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.cam_overlays, null);
        LayoutParams layoutParamsControl = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        );
        this.addContentView(viewControl, layoutParamsControl);

        mSnapshotButton = (Button) findViewById(R.id.snapshotbutton);
        mSnapshotButton.setOnClickListener(
            new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    mCameraSurface.takePicture();
                }
            }
        );

        mGMeter.bindGuiElement((SeekBar) findViewById(R.id.gmeter));
        mSpeedometer.bindGuiElement((TextView) findViewById(R.id.speedometer));
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mOrientationEventListener.enable();

        mGMeter.start();
        mSpeedometer.start();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mOrientationEventListener.disable();

        mGMeter.stop();
        mSpeedometer.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.clear();
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
