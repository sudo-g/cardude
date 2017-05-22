package sudo_g.cardude;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MediaFileManager
{
    private static MediaFileManager singletonMediaFileManager;

    private Context mContext;
    private File mInputFile;

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
    public FileOutputStream getPhotoInputStream() throws IOException
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK);
        String fileName = sdf.format(new Date());
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            String fileNameExt = String.format("/%s.jpg", fileName);
            String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + fileNameExt;
            File file = new File(fullPath);
            Log.d("MediaFileManager", file.getAbsolutePath());
            return new FileOutputStream(file);
        }
        else
        {
            throw new IOException(mContext.getString(R.string.no_ext_storage));
        }
    }

    /**
     * Gets an input stream for writing video data to.
     *
     * @return Stream to write video data to.
     * @throws IOException Error during file creation.
     */
    public FileOutputStream getVideoInputStream() throws IOException
    {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            mInputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/testvideo.mp4");
            Log.d("MediaFileManager", mInputFile.getAbsolutePath());
            mInputFile.createNewFile();
            return new FileOutputStream(mInputFile);
        }
        else
        {
            throw new IOException(mContext.getString(R.string.no_ext_storage));
        }
    }
}
