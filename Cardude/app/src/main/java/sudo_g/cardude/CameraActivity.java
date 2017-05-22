package sudo_g.cardude;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import java.io.IOException;

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
    private final LocationService mLocationService = LocationService.getLocationService(this);
    private final MediaFileManager mFileManager = MediaFileManager.getFileManager(this);

    private CameraSurface.Listener mCameraSurfaceListener = new CameraSurface.Listener()
    {
        public void onTakePictureError(String message)
        {
            mPhotoErrorAlert
                .setMessage(String.format(getString(R.string.photo_alert_body), message))
                .show();
        }
    };
    private CameraSurface mCameraSurface;

    private Button mSnapshotButton;
    private Button mVideoButton;
    private Button mDriveButton;

    private final GMeter mGMeter = new GMeter(this);
    private Speedometer mSpeedometer;
    private AlertDialog.Builder mVideoErrorAlert;
    private AlertDialog.Builder mPhotoErrorAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().setFormat(PixelFormat.UNKNOWN);

        initializeAlertDialogs();

        mOrientationManager = new OrientationManager(this, mOrientationListener);
        mLocationService.start();

        // start camera
        mCameraSurface = (CameraSurface) findViewById(R.id.camerapreview);
        mCameraSurface.start(mCameraSurfaceListener);

        // add overlays
        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.cam_overlays, null);
        LayoutParams layoutParamsControl = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        );
        this.addContentView(viewControl, layoutParamsControl);

        mDriveButton = (Button) findViewById(R.id.start_button);
        mDriveButton.setOnClickListener(
            new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    try
                    {
                        mCameraSurface.startRecordVideo(mFileManager);
                    }
                    catch (IOException e1)
                    {
                        mVideoErrorAlert
                                .setMessage(String.format(getString(R.string.video_alert_body), e1.getMessage()))
                                .show();
                    }
                    catch (IllegalStateException e2)
                    {
                        mVideoErrorAlert
                                .setMessage(String.format(getString(R.string.video_alert_body), e2.getMessage()))
                                .show();
                    }
                }
            }
        );

        mSnapshotButton = (Button) findViewById(R.id.snapshotbutton);
        mSnapshotButton.setOnClickListener(
            new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    // error handling is asynchronous, done by CameraSurface.Listener
                    mCameraSurface.takePicture(mFileManager);
                }
            }
        );

        mVideoButton = (Button) findViewById(R.id.video_button);
        mVideoButton.setOnClickListener(
            new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    mCameraSurface.captureLastVideoBuffer();
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
        mSpeedometer.start(mLocationService);
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

    private void initializeAlertDialogs()
    {
        // create video error dialog
        mVideoErrorAlert = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.video_alert_title))
            .setNeutralButton(
                    android.R.string.yes,
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }
            )
            .setIcon(android.R.drawable.ic_dialog_alert);


        // create photo error dialogs
        mPhotoErrorAlert = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.photo_alert_title))
            .setNeutralButton(
                    android.R.string.yes,
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    }
            )
            .setIcon(android.R.drawable.ic_dialog_alert);
    }
}
