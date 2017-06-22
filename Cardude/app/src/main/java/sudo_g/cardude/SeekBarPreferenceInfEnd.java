package sudo_g.cardude;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreferenceInfEnd extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
    private SeekBar mSeekBar;
    private TextView mValueText;

    public SeekBarPreferenceInfEnd(Context context, AttributeSet attrs)
    {
        super(context, attrs);

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
        else
        {
            mSeekBar.setProgress(videoBufferTime);
            mValueText.setText(String.format("%d", videoBufferTime));
        }

        mSeekBar.setOnSeekBarChangeListener(this);

        super.onBindDialogView(view);
    }

    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch)
    {
        SharedPreferences.Editor prefEditor = getSharedPreferences().edit();
        if (value == mSeekBar.getMax())
        {
            // special case when seek bar is at max to indicate record all video
            mValueText.setText(getContext().getString(R.string.video_buffer_record_all));
        }
        else if (value > 0)
        {
            // don't let 0 be selected because 0s length buffer makes no sense
            mValueText.setText(String.format("%d", value));
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
            prefEditor.putInt("pref_key_video_buffer_time", seekBarProgress);
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
}
