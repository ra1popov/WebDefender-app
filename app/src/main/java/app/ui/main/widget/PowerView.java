package app.ui.main.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import app.R;
import io.ghyeok.stickyswitch.widget.StickySwitch;

public class PowerView extends LinearLayout {

    public interface OnPowerListener {
        void onPower(boolean on);
    }

    private StickySwitch ssPower;

    public PowerView(Context context) {
        super(context);
        init(context);
    }

    public PowerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PowerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_power, this);

        ssPower = findViewById(R.id.ss_power);
    }

    public void setOnPowerListener(@Nullable OnPowerListener listener) {
        ssPower.setOnSelectedChangeListener(new StickySwitch.OnSelectedChangeListener() {
            @Override
            public void onSelectedChange(@NonNull StickySwitch.Direction direction, @NonNull String s) {
                if (listener != null) {
                    listener.onPower(Objects.equals(StickySwitch.Direction.RIGHT, direction));
                }
            }
        });
    }

    public Boolean isPower() {
        return Objects.equals(StickySwitch.Direction.RIGHT, ssPower.getDirection());
    }

    public void setPower(boolean power) {
        ssPower.setDirection(power ? StickySwitch.Direction.RIGHT : StickySwitch.Direction.LEFT);
    }

}