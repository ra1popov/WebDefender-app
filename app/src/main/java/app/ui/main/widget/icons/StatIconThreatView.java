package app.ui.main.widget.icons;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import app.R;


public class StatIconThreatView extends LinearLayout {

    private Context context;

    private ImageView ivStatIconThreat;

    private Animation animStatIconThreat;


    public StatIconThreatView(Context context) {
        super(context);
        init(context);
    }

    public StatIconThreatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StatIconThreatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_stat_icon_threat, this);

        ivStatIconThreat = findViewById(R.id.widget_stat_icon_threat0);

        animStatIconThreat = AnimationUtils.loadAnimation(context, R.anim.widget_stat_icon_threat);
    }

    public void setPowerOn() {
        setTint(ivStatIconThreat, R.color.widget_stat_threat_icon_color);
        ivStatIconThreat.startAnimation(animStatIconThreat);
    }

    public void setPowerOff() {
        setTint(ivStatIconThreat, R.color.widget_stat_off_icon_color);
        ivStatIconThreat.clearAnimation();
    }

    private void setTint(@NonNull ImageView view, @ColorRes int id) {
        DrawableCompat.setTint(view.getDrawable(), ContextCompat.getColor(context, id));
    }

}
