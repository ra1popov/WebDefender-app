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


public class StatIconNoadsView extends LinearLayout {

    private Context context;

    private ImageView ivStatIconNoads0;
    private ImageView ivStatIconNoads1;
    private ImageView ivStatIconNoads2;

    private Animation animStatIconNoads;


    public StatIconNoadsView(Context context) {
        super(context);
        init(context);
    }

    public StatIconNoadsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StatIconNoadsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_stat_icon_noads, this);

        ivStatIconNoads0 = findViewById(R.id.widget_stat_icon_noads0);
        ivStatIconNoads1 = findViewById(R.id.widget_stat_icon_noads1);
        ivStatIconNoads2 = findViewById(R.id.widget_stat_icon_noads2);

        animStatIconNoads = AnimationUtils.loadAnimation(context, R.anim.widget_stat_icon_noads);
    }

    public void setPowerOn() {
        ivStatIconNoads0.setVisibility(INVISIBLE);
        ivStatIconNoads1.setVisibility(VISIBLE);
        ivStatIconNoads2.setVisibility(VISIBLE);
        setTint(ivStatIconNoads1, R.color.widget_stat_noads_icon_color);
        setTint(ivStatIconNoads2, R.color.widget_stat_noads_icon_color);
        ivStatIconNoads2.startAnimation(animStatIconNoads);
    }

    public void setPowerOff() {
        ivStatIconNoads0.setVisibility(VISIBLE);
        ivStatIconNoads1.setVisibility(INVISIBLE);
        ivStatIconNoads2.setVisibility(INVISIBLE);
        setTint(ivStatIconNoads1, R.color.widget_stat_off_icon_color);
        setTint(ivStatIconNoads2, R.color.widget_stat_off_icon_color);
        ivStatIconNoads2.clearAnimation();
    }

    private void setTint(@NonNull ImageView view, @ColorRes int id) {
        DrawableCompat.setTint(view.getDrawable(), ContextCompat.getColor(context, id));
    }

}
