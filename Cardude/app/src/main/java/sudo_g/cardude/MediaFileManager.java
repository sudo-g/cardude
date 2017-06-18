package sudo_g.cardude;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.IOUtils;

public class MediaFileManager
{
    private static final String DEFAULT_PHOTO_PATH_FROM_EXT = "cardude/photos";
    private static final String VIDEO_BUFFER_FROM_EXT = "cardude/~videobuffer";
    private static final String DEFAULT_VIDEO_PATH_FROM_EXT = "cardude/videos";

    private static final int VIDEO_BUFFER_DEPTH = 3;

    private static MediaFileManager singletonMediaFileManager;

    private Context mContext;
    private Queue<File> mVideoBuffers = new ArrayDeque<File>(VIDEO_BUFFER_DEPTH);
    private Lock mVideoBufferLock = new ReentrantLock();    // prevent race conditions for queue access


    /**
     * Gets a handle to the file manager.
     *
     * @param context Application context.
     * @return Handle to the manager.
     */
    public static MediaFileManager getFileManager(Context context)
    {
        if (singletonMediaFileManager == null)
        {
            singletonMediaFileManager = new MediaFileManager(context);
        }
        return singletonMediaFileManager;
    }

    private MediaFileManager(Context context)
    {
        mContext = context;
    }

    /**
     * Gets an input stream for writing photo data to.
     *
     * @return Stream to write photo data to.
     * @throws IOException Error during file creation.
     */
    public FileOutputStream getNewPhotoFileStream() throws IOException
    {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            File photoStorageLocation = createDirIfNoExists(DEFAULT_PHOTO_PATH_FROM_EXT);
            if (photoStorageLocation != null)
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK);
                String fileName = sdf.format(new Date());
                String photoStorageLocationPath = photoStorageLocation.getAbsolutePath();
                String fileNameExt = String.format("/%s.jpg", fileName);
                String fullPath = String.format("%s/%s", photoStorageLocationPath, fileNameExt);
                File file = new File(fullPath);
                return new FileOutputStream(file);
            }
            else
            {
                throw new IOException(mContext.getString(R.string.photo_dir_error));
            }
        }
        else
        {
            throw new IOException(mContext.getString(R.string.no_ext_storage));
        }
    }

    /**
     * Gets an input stream for writing video data to.
     *
     * @return File descriptor of file to write video to.
     * @throws IOException Error during file creation.
     */
    public File getNewVideoBufferFD() throws IOException
    {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            File videoBufferLocation = createDirIfNoExists(VIDEO_BUFFER_FROM_EXT);
            if (videoBufferLocation != null)
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK);
                String fileName = sdf.format(new Date());
                String videoBufferLocationPath = videoBufferLocation.getAbsolutePath();
                String fileNameExt = String.format("/%s.mp4", fileName);
                String fullPath = String.format("%s/%s", videoBufferLocationPath, fileNameExt);

                File newBufferSlotFd = null;
                File toDeleteFd = null;

                mVideoBufferLock.lock();
                try
                {
                    if (mVideoBuffers.add(new File(fullPath)))
                    {
                        newBufferSlotFd = mVideoBuffers.peek();
                    }
                    else
                    {
                        // video buffer depth full, remove oldest before adding new
                        toDeleteFd = mVideoBuffers.remove();
                        toDeleteFd.delete();
                        mVideoBuffers.add(new File(fullPath));
                        newBufferSlotFd = mVideoBuffers.peek();
                    }
                }
                finally
                {
                    mVideoBufferLock.unlock();
                }

                return newBufferSlotFd;
            }
            else
            {
                throw new IOException(mContext.getString(R.string.vid_buffer_dir_error));
            }
        }
        else
        {
            throw new IOException(mContext.getString(R.string.no_ext_storage));
        }
    }

    public void clearVideoBuffer()
    {
        for (File f: mVideoBuffers)
        {
            f.delete();
        }
    }

    public FileOutputStream getNewVideoVideoFileStream() throws IOException
    {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            File videoStorageLocation = createDirIfNoExists(DEFAULT_VIDEO_PATH_FROM_EXT);
            if (videoStorageLocation != null)
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK);
                String fileName = sdf.format(new Date());
                String videoStorageLocationPath = videoStorageLocation.getAbsolutePath();
                String fileNameExt = String.format("/%s.mp4", fileName);
                String fullPath = String.format("%s/%s", videoStorageLocationPath, fileNameExt);
                Log.d("MediaFileManager", fullPath);
                File file = new File(fullPath);
                return new FileOutputStream(file);
            }
            else
            {
                throw new IOException(mContext.getString(R.string.video_dir_error));
            }
        }
        else
        {
            throw new IOException(mContext.getString(R.string.no_ext_storage));
        }
    }

    /**
     * Permanently saves the latest content from the video buffer.
     */
    public void saveVideoBuffer()
    {
        Thread backgroundCopyTask = new Thread()
        {
            public void run()
            {
                try
                {
                    mVideoBufferLock.lock();
                    File fileToKeep = mVideoBuffers.remove();
                    mVideoBufferLock.unlock();

                    InputStream in = new FileInputStream(fileToKeep);
                    OutputStream out = getNewVideoVideoFileStream();
                    IOUtils.copy(in, out);

                    in.close();
                    out.close();

                    fileToKeep.delete();
                }
                catch (IOException e)
                {
                    // Copy job fails
                    // As it occurs on another thread, it's best not to do anything.
                }
            }
        };
        backgroundCopyTask.start();
    }

    private File createDirIfNoExists(String directory)
    {
        String dirPath = String.format("%s/%s",
                Environment.getExternalStorageDirectory().getAbsoluteFile(),
                directory
        );
        File dir = new File(dirPath);
        if (!dir.exists())
        {
            if (dir.mkdirs())
            {
                return dir;
            }
        }
        else
        {
            return dir;
        }
        return null;
    }
}
