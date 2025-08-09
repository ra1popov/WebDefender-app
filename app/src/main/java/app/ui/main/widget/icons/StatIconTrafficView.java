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


public class StatIconTrafficView extends LinearLayout {

    private Context context;

    private ImageView ivStatIconTraffic0;
    private ImageView ivStatIconTraffic1;
    private ImageView ivStatIconTraffic2;

    private Animation animStatIconTraffic1;
    private Animation animStatIconTraffic2;


    public StatIconTrafficView(Context context) {
        super(context);
        init(context);
    }

    public StatIconTrafficView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StatIconTrafficView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_stat_icon_traffic, this);

        ivStatIconTraffic0 = findViewById(R.id.widget_stat_icon_traffic0);
        ivStatIconTraffic1 = findViewById(R.id.widget_stat_icon_traffic1);
        ivStatIconTraffic2 = findViewById(R.id.widget_stat_icon_traffic2);

        animStatIconTraffic1 = AnimationUtils.loadAnimation(context, R.anim.widget_stat_icon_traffic1);
        animStatIconTraffic2 = AnimationUtils.loadAnimation(context, R.anim.widget_stat_icon_traffic2);
    }

    public void setPowerOn() {
        ivStatIconTraffic0.setVisibility(INVISIBLE);
        ivStatIconTraffic1.setVisibility(VISIBLE);
        ivStatIconTraffic2.setVisibility(VISIBLE);
        setTint(ivStatIconTraffic1, R.color.widget_stat_traffic_icon_color);
        setTint(ivStatIconTraffic2, R.color.widget_stat_traffic_icon_color);
        ivStatIconTraffic1.startAnimation(animStatIconTraffic1);
        ivStatIconTraffic2.startAnimation(animStatIconTraffic2);
    }

    public void setPowerOff() {
        ivStatIconTraffic0.setVisibility(VISIBLE);
        ivStatIconTraffic1.setVisibility(INVISIBLE);
        ivStatIconTraffic2.setVisibility(INVISIBLE);
        setTint(ivStatIconTraffic1, R.color.widget_stat_off_icon_color);
        setTint(ivStatIconTraffic2, R.color.widget_stat_off_icon_color);
        ivStatIconTraffic1.clearAnimation();
        ivStatIconTraffic2.clearAnimation();
    }

    private void setTint(@NonNull ImageView view, @ColorRes int id) {
        DrawableCompat.setTint(view.getDrawable(), ContextCompat.getColor(context, id));
    }

}
