package sudo_g.cardude;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import sudo_g.cardude.OrientationManager.DeviceOrientation;

public class CameraSurface extends SurfaceView
{
    private DeviceOrientation mCurrentRotation = DeviceOrientation.PORTRAIT;

    private volatile boolean previewing = false;
    private int mCamIndex = -1;
    private Camera mCamera;
    private List mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;

    private SurfaceHolder mSurfaceHolder;
    private final SurfaceHolder.Callback mSurfaceHolderEvents = new SurfaceHolder.Callback()
    {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
        {
            if (previewing)
            {
                mCapturePreviewLock.lock();
                mCamera.stopPreview();
                previewing = false;
                mCapturePreviewLock.unlock();
            }

            if (mCamera != null)
            {
                mCamera.setDisplayOrientation(getCorrectCameraRotation(mCurrentRotation, mCamIndex));
                if (mPreviewSize != null)
                {
                    Camera.Parameters parameters = mCamera.getParameters();
                    parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                    mCamera.setParameters(parameters);
                }

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
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
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

    private final Lock mCapturePreviewLock = new ReentrantLock();    // keeps preview flag synchronized
    private final Camera.PictureCallback mCaptureJpegEvent = new Camera.PictureCallback()
    {
        public void onPictureTaken(byte[] data, Camera camera)
        {
            mCapturePreviewLock.lock();
            mCamera.startPreview();
            previewing = true;
            mCapturePreviewLock.unlock();
        }
    };

    public CameraSurface(Context context)
    {
        super(context);
        mCamIndex = findBackFacingCameraIndex();
        mSurfaceHolder = getHolder();
    }

    public CameraSurface(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mCamIndex = findBackFacingCameraIndex();
        mSurfaceHolder = getHolder();
    }

    public CameraSurface(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        mCamIndex = findBackFacingCameraIndex();
        mSurfaceHolder = getHolder();
    }

    /**
     * Setup all other resources required by this view.
     */
    public void start()
    {
        mSurfaceHolder.addCallback(mSurfaceHolderEvents);
    }

    public void stop()
    {
        mSurfaceHolder.removeCallback(mSurfaceHolderEvents);
    }

    /**
     * Rotate the camera preview given the device orientation.
     *
     * @param rotation Description of the rotation angle.
     */
    public void setRotationAngle(OrientationManager.DeviceOrientation rotation)
    {
        mCurrentRotation = rotation;
    }

    public boolean takePicture()
    {
        if (!previewing)
        {
            return false;
        }
        else
        {
            mCapturePreviewLock.lock();
            mCamera.takePicture(null, null, mCaptureJpegEvent);
            previewing = false;    // preview stops when picture is taken
            mCapturePreviewLock.unlock();

            return true;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (mSupportedPreviewSizes != null)
        {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }

        if (mPreviewSize != null)
        {
            float ratio;
            if(mPreviewSize.height >= mPreviewSize.width)
            {
                ratio = (float) mPreviewSize.height / (float) mPreviewSize.width;
            }
            else
            {
                ratio = (float) mPreviewSize.width / (float) mPreviewSize.height;
            }

            if (mCurrentRotation == DeviceOrientation.PORTRAIT || mCurrentRotation == DeviceOrientation.REVERSE_PORTRAIT)
            {
                setMeasuredDimension(width, (int) (width * ratio));
            }
            else
            {
                setMeasuredDimension(width, (int) (width / ratio));
            }
        }
        else
        {
            // reverting to default view behavior
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
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

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h)
    {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // find the best preview size within the aspect ratio tolerance
        for (Camera.Size size : sizes)
        {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
            {
                continue;
            }

            if (Math.abs(size.height - targetHeight) < minDiff)
            {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // if none in tolerance, find the best based on height
        if (optimalSize == null)
        {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes)
            {
                if (Math.abs(size.height - targetHeight) < minDiff)
                {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    private int getCorrectCameraRotation(DeviceOrientation rotation, int camIndex)
    {
        int degrees = 0;
        switch (rotation)
        {
            case PORTRAIT:
                degrees = 0;
                break;
            case LANDSCAPE:
                degrees = 90;
                break;
            case REVERSE_PORTRAIT:
                degrees = 180;
                break;
            case REVERSE_LANDSCAPE:
                degrees = 270;
                break;
        }

        CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(camIndex, info);

        return (info.orientation - degrees + 360) % 360;
    }
}
