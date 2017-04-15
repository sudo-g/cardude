package sudo_g.cardude;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraSurface
{
    private final Activity mParentActivity;

    private boolean previewing = false;
    private int mCamIndex = -1;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceHolder.Callback mSurfaceHolderEvents = new SurfaceHolder.Callback()
    {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
        {
            if (previewing)
            {
                mCamera.stopPreview();
                previewing = false;
            }

            if (mCamera != null) {
                try
                {
                    mCamera.setPreviewDisplay(mSurfaceHolder);
                    mCamera.startPreview();
                    previewing = true;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder)
        {
            mCamera = Camera.open(mCamIndex);

            int rotation = mParentActivity.getWindowManager().getDefaultDisplay().getRotation();
            setCameraToCurrentOrientation(rotation);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder)
        {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            previewing = false;
        }
    };

    /**
     * Creates the camera surface, call inside onCreate() of Activity.
     *
     * @param activity Parent activity creating this object.
     */
    public CameraSurface(Activity activity)
    {
        mParentActivity = activity;

        mCamIndex = findBackFacingCameraIndex();

        mSurfaceView = (SurfaceView) mParentActivity.findViewById(R.id.camerapreview);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(mSurfaceHolderEvents);
    }

    private int findBackFacingCameraIndex()
    {
        int numOfCameras = Camera.getNumberOfCameras();
        int retCamIndex = -1;
        int searchCamIndex = 0;

        while (retCamIndex == -1 && searchCamIndex < numOfCameras)
        {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(searchCamIndex, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK)
            {
                retCamIndex = searchCamIndex;
            }
            else
            {
                searchCamIndex++;
            }
        }
        return retCamIndex;
    }

    private void setCameraToCurrentOrientation(int rotation)
    {
        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCamIndex, info);

        int result = (info.orientation - degrees + 360) % 360;

        mCamera.setDisplayOrientation(result);
    }
}
