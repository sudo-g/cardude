package sudo_g.cardude;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;

import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import sudo_g.cardude.OrientationManager.DeviceOrientation;

public class CameraSurface extends SurfaceView
{
    public interface Listener
    {
        /**
         * Called when error occurs on taking a still photo.
         *
         * @param message Human readable error message.
         */
        public void onTakePictureError(String message);

        /**
         * Called when error occurs on saving the video buffer.
         *
         * @param message Human readable error message.
         */
        public void onSaveVideoBufferError(String message);
    }

    private static int DEFAULT_CIRCULAR_BUFFER_LENGTH_MS = 20000;
    private int mCircularBufferLengthMs = DEFAULT_CIRCULAR_BUFFER_LENGTH_MS;

    private DeviceOrientation mCurrentRotation = DeviceOrientation.PORTRAIT;

    private volatile boolean previewing = false;
    private int mCamIndex = -1;
    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;

    private boolean mRecording = false;
    private MediaRecorder mMediaRecorder;
    private List<Camera.Size> mSupportedVideoSizes;
    private Camera.Size mSelectedVideoSize;
    private MediaFileManager mMediaFileManager;     // TODO: Change circular buffer implementation
    private boolean mSaveBufferRequested = false;
    private Handler mVideoBufferHandler = new Handler();
    private Runnable mVideoShuffleTask = new Runnable()
    {
        @Override
        public void run()
        {
            mMediaRecorder.stop();
            if (mSaveBufferRequested)
            {
                mMediaFileManager.saveVideoBuffer();
                mSaveBufferRequested = false;
            }
            try
            {
                prepareMediaRecorder(mMediaFileManager);
                if (mRecording && mCircularBufferLengthMs > 0)
                {
                    mVideoBufferHandler.postDelayed(this, mCircularBufferLengthMs);
                }
            }
            catch (IOException e)
            {
                // this won't be how circular video buffer is implemented in long term
                e.printStackTrace();
            }

        }
    };

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
            mSupportedVideoSizes = mCamera.getParameters().getSupportedVideoSizes();
            // automatically selecting the first for now, generally highest res available
            mSelectedVideoSize = mSupportedVideoSizes.get(0);
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

    private Listener mListener;

    private final Lock mCapturePreviewLock = new ReentrantLock();    // keeps preview flag synchronized

    public CameraSurface(Context context)
    {
        super(context);
        initialize();
    }

    public CameraSurface(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize();
    }

    public CameraSurface(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize();
    }

    /**
     * Setup all other resources required by this view.
     *
     * @param listener Callback for this class' events.
     */
    public void start(Listener listener)
    {
        mSurfaceHolder.addCallback(mSurfaceHolderEvents);
        mListener = listener;
    }

    public void stop()
    {
        stopRecordVideo();
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

    public boolean takePicture(final MediaFileManager fileManager)
    {
        if (!previewing)
        {
            return false;
        }
        else
        {
            mCapturePreviewLock.lock();
            mCamera.takePicture(null, null,
                new Camera.PictureCallback()
                {
                    public void onPictureTaken(byte[] data, Camera camera)
                    {
                        mCapturePreviewLock.lock();
                        mCamera.startPreview();
                        previewing = true;
                        mCapturePreviewLock.unlock();

                        try
                        {
                            FileOutputStream stream = fileManager.getNewPhotoFileStream();
                            if (stream != null)
                            {
                                stream.write(data);
                                stream.close();
                            }
                        }
                        catch (IOException e)
                        {
                            if (mListener != null)
                            {
                                mListener.onTakePictureError(e.getMessage());
                            }
                        }
                    }
                }
            );
            previewing = false;    // preview stops when picture is taken
            mCapturePreviewLock.unlock();

            return true;
        }
    }

    /**
     * Start video capture to buffer.
     *
     * @param fileManager
     * @throws IOException if failed to create file for recording.
     * @throws IllegalStateException if camera instance is invalid.
     */
    public void startRecordVideo(MediaFileManager fileManager) throws IOException, IllegalStateException
    {
        mRecording = true;
        prepareMediaRecorder(fileManager);
        if (mCircularBufferLengthMs > 0)
        {
            mVideoBufferHandler.postDelayed(mVideoShuffleTask, mCircularBufferLengthMs);
        }
    }

    /**
     * Saves the latest content in the video buffer.
     *
     * @throws IOException
     */
    public void captureLastVideoBuffer() throws IOException
    {
        mSaveBufferRequested = true;
    }

    public void stopRecordVideo()
    {
        mRecording = false;
        mVideoBufferHandler.removeCallbacks(mVideoShuffleTask);
        try
        {
            mMediaRecorder.stop();
            if (mSaveBufferRequested)
            {
                mMediaFileManager.saveVideoBuffer();
                mSaveBufferRequested = false;
            }
        }
        catch (RuntimeException e)
        {
            // no video was recorded when request stopped
        }

        mMediaFileManager.clearVideoBuffer();
        mMediaFileManager = null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (mSupportedPreviewSizes != null)
        {
            double targetAspect = (double) mSelectedVideoSize.height / mSelectedVideoSize.width;
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height, targetAspect);
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

    private void prepareMediaRecorder(MediaFileManager fileManager) throws IOException
    {
        if (mCamera != null)
        {
            mMediaFileManager = fileManager;
            mMediaRecorder = new MediaRecorder();
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            profile.videoFrameWidth = mSelectedVideoSize.width;
            profile.videoFrameHeight = mSelectedVideoSize.height;
            mMediaRecorder.setProfile(profile);

            try
            {
                mMediaRecorder.setOutputFile(mMediaFileManager.getNewVideoBufferFD().getAbsolutePath());
                mMediaRecorder.prepare();
            }
            catch (IOException fileErr)
            {
                mCamera.reconnect();
                throw fileErr;
            }

            mMediaRecorder.start();
        }
        else
        {
            throw new IllegalStateException(getContext().getString(R.string.no_cam_instance));
        }
    }

    private void initialize()
    {
        // determine buffer recording time
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String strCircularBufferLength = pref.getString(
            "pref_key_video_buffer_time",
             String.format("%d", DEFAULT_CIRCULAR_BUFFER_LENGTH_MS)
        );
        // stored in shared preferences as seconds
        mCircularBufferLengthMs = 1000*Integer.parseInt(strCircularBufferLength);

        mCamIndex = findBackFacingCameraIndex();
        mSurfaceHolder = getHolder();
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

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, final int w, final int h, final double aspect)
    {
        if (sizes == null)
        {
            return null;
        }

        // most suitable first
        Comparator<Camera.Size> previewSizeSorter = new Comparator<Camera.Size>()
        {
            @Override
            public int compare(Camera.Size size1, Camera.Size size2)
            {
                // if aspect ratio within tolerance, width by height
                int heightDiff1 = Math.abs(size1.height - h);
                int heightDiff2 = Math.abs(size2.height - h);

                double aspectDiff1 = Math.abs((double) size1.height / size1.width - aspect);
                double aspectDiff2 = Math.abs((double) size2.height / size2.width - aspect);

                if (aspectDiff1 < 0.1 && aspectDiff2 < 0.1)
                {
                    return heightDiff1 - heightDiff2;
                }
                else
                {
                    return (int) Math.signum(aspectDiff1 - aspectDiff2);
                }
            }
        };

        Collections.sort(sizes, previewSizeSorter);

        return sizes.get(0);
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
