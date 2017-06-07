package sudo_g.cardude;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MediaFileManager
{
    private static final String DEFAULT_PHOTO_PATH_FROM_EXT = "cardude/photos";

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
            String photoStorageLocationPath = String.format("%s/%s",
                    Environment.getExternalStorageDirectory().getAbsoluteFile(),
                    DEFAULT_PHOTO_PATH_FROM_EXT
            );
            File photoStorageLocation = new File(photoStorageLocationPath);
            if (!photoStorageLocation.exists())
            {
                if (photoStorageLocation.mkdirs())
                {
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
        }
        else
        {
            throw new IOException(mContext.getString(R.string.no_ext_storage));
        }

        return null;
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
            mInputFile.createNewFile();
            return new FileOutputStream(mInputFile);
        }
        else
        {
            throw new IOException(mContext.getString(R.string.no_ext_storage));
        }
    }
}
