package sudo_g.cardude;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;

import java.io.IOException;

public class CameraActivity extends ActionBarActivity {

    LayoutInflater controlInflater = null;

    private OrientationManager mOrientationManager;
    private OrientationManager.Listener mOrientationListener = new OrientationManager.Listener()
    {
        public void onOrientationChanged(OrientationManager.DeviceOrientation orientation)
        {
            mCameraSurface.setRotationAngle(orientation);
            mDriveControls.setRotationAngle(orientation);
        }
    };

    private final LocationService mLocationService = LocationService.getLocationService(this);
    private final MediaFileManager mFileManager = MediaFileManager.getFileManager(this);

    private CameraSurface mCameraSurface;
    private CameraSurface.Listener mCameraSurfaceListener = new CameraSurface.Listener()
    {
        public void onTakePictureError(String message)
        {
            mPhotoErrorAlert
                .setMessage(String.format(getString(R.string.photo_alert_body), message))
                .show();
        }
    };

    private DriveControls mDriveControls;
    private DriveControls.Listener mDriveControlsListener = new DriveControls.Listener()
    {
        public void takePictureRequest()
        {
            // error handling is asynchronous, done by CameraSurface.Listener
            mCameraSurface.takePicture(mFileManager);
        }

        public void captureLastVideoBufferRequest()
        {
            // stop the video for now
        }

        public void onDriveStart()
        {
            try
            {
                mCameraSurface.startRecordVideo(mFileManager);
            }
            catch (IOException fileErr)
            {
                mVideoErrorAlert
                        .setMessage(String.format(getString(R.string.video_alert_body), fileErr.getMessage()))
                        .show();
            }
            catch (IllegalStateException camErr)
            {
                mVideoErrorAlert
                        .setMessage(String.format(getString(R.string.video_alert_body), camErr.getMessage()))
                        .show();
            }
        }
    };

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

        // start camera
        mCameraSurface = (CameraSurface) findViewById(R.id.camera_preview);
        mCameraSurface.start(mCameraSurfaceListener);

        // add overlays
        mDriveControls = (DriveControls) findViewById(R.id.drive_controls);
        mDriveControls.setEventListener(mDriveControlsListener);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mLocationService.start();
        mOrientationManager.start();
        mDriveControls.start(mLocationService);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mDriveControls.stop();
        mOrientationManager.stop();
        mLocationService.stop();
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
