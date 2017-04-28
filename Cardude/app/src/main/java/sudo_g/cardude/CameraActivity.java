package sudo_g.cardude;

import android.graphics.PixelFormat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.SeekBar;

public class CameraActivity extends ActionBarActivity {

    LayoutInflater controlInflater = null;

    private OrientationManager.Listener mOrientationListener = new OrientationManager.Listener()
    {
        public void onOrientationChanged(OrientationManager.DeviceOrientation orientation)
        {
            mCameraSurface.setRotationAngle(orientation);
            mGMeter.setRotationAngle(orientation);
        }
    };
    private OrientationManager mOrientationManager;

    private CameraSurface mCameraSurface;
    private Button mSnapshotButton;
    private final GMeter mGMeter = new GMeter(this);
    private Speedometer mSpeedometer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().setFormat(PixelFormat.UNKNOWN);

        mOrientationManager = new OrientationManager(this, mOrientationListener);

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
        mSpeedometer = (Speedometer) findViewById(R.id.speedometer);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mOrientationManager.start();

        mGMeter.start();
        mSpeedometer.start();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mOrientationManager.stop();

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
