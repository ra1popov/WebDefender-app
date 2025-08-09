package app.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import app.R;
import app.util.SafeOnClickListener;

public class ToolbarView extends Toolbar {

    private Context context;

    private RelativeLayout rlIconLeft;
    private ImageView ivIconLeft;

    private RelativeLayout rlIconRight;
    private ImageView ivIconRight;

    private TextView tvTitle;

    private boolean home = true;
    private String title;


    public ToolbarView(Context context) {
        super(context);
        init(context);
    }

    public ToolbarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ToolbarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_toolbar, this);

        setContentInsetsAbsolute(0, 0); // Remove left and right padding so that the title is perfectly centered.

        rlIconLeft = findViewById(R.id.rl_icon_left);
        ivIconLeft = findViewById(R.id.iv_icon_left);
        rlIconRight = findViewById(R.id.rl_icon_right);
        ivIconRight = findViewById(R.id.iv_icon_right);
        tvTitle = findViewById(R.id.tv_title);

        ivIconLeft.setImageResource(R.drawable.ic_toolbar_back);
        ivIconRight.setImageResource(R.drawable.ic_toolbar_menu);

        fillComponent();
    }

    private void fillComponent() {
        if (home) {
            rlIconLeft.setVisibility(GONE);
            tvTitle.setVisibility(GONE);
        } else {
            rlIconLeft.setVisibility(VISIBLE);
            tvTitle.setVisibility(VISIBLE);
        }

        if (!TextUtils.isEmpty(title)) {
            tvTitle.setText(title);
        }
    }

    public void setHome(boolean home) {
        this.home = home;
        fillComponent();
    }

    public boolean isHome() {
        return home;
    }

    public void setTitle(@StringRes int resId) {
        this.title = getResources().getText(resId).toString();
        fillComponent();
    }

    public void setTitle(String title) {
        this.title = title;
        fillComponent();
    }

    public void setOnLeftClickListener(@Nullable OnClickListener listener) {
        this.rlIconLeft.setOnClickListener(new SafeOnClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                if (listener != null) {
                    listener.onClick(view);
                }
            }
        });
    }

    public void setOnRightClickListener(@Nullable OnClickListener listener) {
        if (listener == null) {
            this.rlIconRight.setVisibility(GONE);
        } else {
            this.rlIconRight.setVisibility(VISIBLE);
        }

        this.rlIconRight.setOnClickListener(new SafeOnClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                if (listener != null) {
                    listener.onClick(view);
                }
            }
        });
    }

    private void setTint(@NonNull TextView view, @ColorRes int id) {
        view.setTextColor(ContextCompat.getColor(context, id));
    }

    //////////////////////////////////////////////////////////////////////////////////////

    private static final String STATE = "state";
    private static final String HOME = "home";
    private static final String TITLE = "title";

    @Override
    public Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(STATE, super.onSaveInstanceState());
        bundle.putBoolean(HOME, home);
        bundle.putString(TITLE, title);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            home = bundle.getBoolean(HOME);
            title = bundle.getString(TITLE);
            fillComponent();
            super.onRestoreInstanceState(bundle.getParcelable(STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }

}
