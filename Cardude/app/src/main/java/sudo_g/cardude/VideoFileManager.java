package sudo_g.cardude;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class VideoFileManager
{
    private Context mContext;
    private File mInputFile;

    public VideoFileManager(Context context)
    {
        mContext = context;
    }

    public FileDescriptor getInputStream() throws IOException
    {

        mInputFile = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/testvideo.mp4");
        FileInputStream fos = new FileInputStream(mInputFile);
        return fos.getFD();
    }
}
