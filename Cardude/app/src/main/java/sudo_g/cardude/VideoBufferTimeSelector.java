package sudo_g.cardude;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class VideoBufferTimeSelector extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
    private final int MAX_VID_BUFFER_TIME;

    private SeekBar mSeekBar;
    private TextView mValueText;

    public VideoBufferTimeSelector(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        MAX_VID_BUFFER_TIME = getContext().getResources().getInteger(R.integer.video_buffer_max_s);
        setDialogLayoutResource(R.layout.seekbar_and_number);
    }

    @Override
    protected void onBindDialogView(View view)
    {
        mSeekBar = (SeekBar) view.findViewById(R.id.value_slider);
        mValueText = (TextView) view.findViewById(R.id.value_text);

        // preset the seek bar to the preference value
        SharedPreferences pref = getSharedPreferences();
        int videoBufferTime = pref.getInt("pref_key_video_buffer_time", -1);
        if (videoBufferTime == 0)
        {
            mSeekBar.setProgress(mSeekBar.getMax());
            mValueText.setText(getContext().getString(R.string.video_buffer_record_all));
        }
        else if (videoBufferTime > 0)
        {
            mSeekBar.setProgress(reverseSeekBarValueMapping(videoBufferTime));
            mValueText.setText(String.format("%d", videoBufferTime));
        }

        // at the moment, nothing happens if the preference cannot be found

        mSeekBar.setOnSeekBarChangeListener(this);

        super.onBindDialogView(view);
    }

    @Override
    public void onProgressChanged(SeekBar seek, int seekBarValue, boolean fromTouch)
    {
        SharedPreferences.Editor prefEditor = getSharedPreferences().edit();
        if (seekBarValue == mSeekBar.getMax())
        {
            // special case when seek bar is at max to indicate record all video
            mValueText.setText(getContext().getString(R.string.video_buffer_record_all));
        }
        else if (seekBarValue > 0)
        {
            int actualValue = applySeekBarValueMapping(seekBarValue);

            // don't let 0 be selected because 0s length buffer makes no sense
            mValueText.setText(String.format("%d", actualValue));
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        if(!positiveResult)
        {
            return;
        }

        int seekBarProgress = mSeekBar.getProgress();
        SharedPreferences.Editor prefEditor = getSharedPreferences().edit();
        if (seekBarProgress == mSeekBar.getMax())
        {
            // use 0 as the value of the preference as infinite
            prefEditor.putInt("pref_key_video_buffer_time", 0);
            prefEditor.commit();
        }
        else if (seekBarProgress > 0)
        {
            // don't write 0 to value of preference if seek bar is dragged left
            prefEditor.putInt("pref_key_video_buffer_time", applySeekBarValueMapping(seekBarProgress));
            prefEditor.commit();
        }

        super.onDialogClosed(positiveResult);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seek)
    {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seek)
    {

    }

    private int applySeekBarValueMapping(int seekBarValue)
    {
        int seekBarMax = mSeekBar.getMax();
        return MAX_VID_BUFFER_TIME * seekBarValue / seekBarMax;
    }

    private int reverseSeekBarValueMapping(int videoBufferTime)
    {
        return mSeekBar.getMax() * videoBufferTime / MAX_VID_BUFFER_TIME;
    }
}
