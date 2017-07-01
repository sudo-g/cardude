package sudo_g.cardude;

import android.content.Context;
import android.hardware.Camera;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.List;

public class VideoResSelector extends DialogPreference
{
    private ListView mResolutionSelector;

    public VideoResSelector(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public View onCreateDialogView()
    {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        mResolutionSelector = new ListView(getContext());
        mResolutionSelector.setLayoutParams(layoutParams);

        LinearLayout parentView = new LinearLayout(getContext());
        parentView.addView(mResolutionSelector);

        return parentView;
    }

    @Override
    public void onBindDialogView(View view)
    {
        super.onBindDialogView(view);

        List<Camera.Size> resolutions = getSupportedResolutions();
        String[] resolutionLabels = new String[resolutions.size()];
        for (int i=0; i<resolutions.size(); i++)
        {
            int width = resolutions.get(i).width;
            int height = resolutions.get(i).height;
            resolutionLabels[i] = String.format("%d x %d", width, height);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            getContext(),
            R.layout.single_textview,
            resolutionLabels
        );
        mResolutionSelector.setAdapter(adapter);
    }

    private List<Camera.Size> getSupportedResolutions()
    {
        int numOfCameras = Camera.getNumberOfCameras();
        int retCamIndex = -1;
        int searchCamIndex = 0;

        while (retCamIndex == -1 && searchCamIndex < numOfCameras)
        {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(searchCamIndex, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            {
                retCamIndex = searchCamIndex;
            }
            else
            {
                searchCamIndex++;
            }
        }

        Camera camera = Camera.open(retCamIndex);
        List<Camera.Size> resolutions = camera.getParameters().getSupportedVideoSizes();
        camera.release();

        return resolutions;
    }
}
