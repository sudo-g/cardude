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
        if (rotation == Surface.ROTATION_0)
        {
            mCamera.setDisplayOrientation(0);
        }
        else if (rotation == Surface.ROTATION_90)
        {
            mCamera.setDisplayOrientation(90);
        }
        else if (rotation == Surface.ROTATION_180)
        {
            mCamera.setDisplayOrientation(180);
        }
        else if (rotation == Surface.ROTATION_270)
        {
            mCamera.setDisplayOrientation(270);
        }

        mCamera.setDisplayOrientation(0);
    }
}
