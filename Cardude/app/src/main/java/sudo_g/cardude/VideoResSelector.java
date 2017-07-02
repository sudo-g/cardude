package sudo_g.cardude;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.List;

public class VideoResSelector extends DialogPreference
{
    private RadioGroup mResolutionSelector;

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

        mResolutionSelector = new RadioGroup(getContext());
        mResolutionSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
            {
               @Override
               public void onCheckedChanged(RadioGroup radioGroup, int i)
               {

               }
            }
        );
        mResolutionSelector.setLayoutParams(layoutParams);

        LinearLayout parentView = new LinearLayout(getContext());
        parentView.addView(mResolutionSelector);

        return parentView;
    }

    @Override
    public void onBindDialogView(View view)
    {
        super.onBindDialogView(view);

        // build menu by querying camera hardware for available options
        List<Camera.Size> resolutions = getSupportedResolutions();
        int selectionOrder = 0;
        for (Camera.Size resolution : resolutions)
        {
            int width = resolution.width;
            int height = resolution.height;
            String radioLabel = String.format("%d x %d", width, height);

            RadioButton rb = new RadioButton(getContext());
            rb.setText(radioLabel);
            rb.setId(selectionOrder++);
            mResolutionSelector.addView(rb);
        }

        // indicate what resolution has currently been selected if any
        SharedPreferences pref = getSharedPreferences();
        int vidResIndex = pref.getInt("pref_key_video_res", -1);
        if (vidResIndex > -1)
        {
            mResolutionSelector.check(vidResIndex);
        }
        else
        {
            // pre-select the highest resolution by default (not committed until "OK" is pressed)
            mResolutionSelector.check(0);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        if (positiveResult)
        {
            int selectedIndex = mResolutionSelector.getCheckedRadioButtonId();
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putInt("pref_key_video_res", selectedIndex);
            editor.commit();
        }
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
