package app.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.R;

public class BlockableLinearLayout extends LinearLayout {

    private boolean clickable = true;
    private boolean active = true;

    public BlockableLinearLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    public BlockableLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        obtainAttributes(attrs);
        init(context);
    }

    public BlockableLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        obtainAttributes(attrs);
        init(context);
    }

    public BlockableLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        obtainAttributes(attrs);
        init(context);
    }

    private void init(Context context) {
        fillComponent();
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public void setActive(boolean active) {
        this.active = active;
        fillComponent();
    }

    public boolean isClickable() {
        return clickable;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (clickable) {
            return super.onTouchEvent(event);
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (clickable) {
            return super.onInterceptTouchEvent(ev);
        }
        return true;
    }

    private void fillComponent() {

        final TypedValue value = new TypedValue();
        if (active) {
            getResources().getValue(R.dimen.alpha_layout_active, value, true);
        } else {
            getResources().getValue(R.dimen.alpha_layout_inactive, value, true);
        }
        setAlpha(value.getFloat());

    }

    //////////////////////////////////////////////////////////////////////////////////////

    private static final String STATE = "state";
    private static final String CLICKABLE = "clickable";
    private static final String ACTIVE = "active";

    @Override
    public Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(STATE, super.onSaveInstanceState());
        bundle.putBoolean(CLICKABLE, clickable);
        bundle.putBoolean(ACTIVE, active);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            clickable = bundle.getBoolean(CLICKABLE, true);
            active = bundle.getBoolean(ACTIVE, true);

            fillComponent();
            super.onRestoreInstanceState(bundle.getParcelable(STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }

    protected void obtainAttributes(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.BlockableLinearLayout);
        clickable = ta.getBoolean(R.styleable.BlockableLinearLayout_clickable, true);
        active = ta.getBoolean(R.styleable.BlockableLinearLayout_active, true);
        ta.recycle();
    }

}
