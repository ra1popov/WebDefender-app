package app.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import java.util.HashMap;

import app.R;

@Keep
public abstract class BaseDialog3<T, BD extends ViewBinding, B extends BuilderDialog<T>> extends DialogFragment {

    protected B builder;
    protected BD binding;

    public BaseDialog3() {
        super();
    }

    public BaseDialog3(B builder) {
        this.builder = builder;
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        String t = getClass().getSimpleName();
        if (tag != null) {
            t = tag;
        }
        if (manager.findFragmentByTag(t) == null) {
            try {
                super.show(manager, t);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    @Override
    public void dismiss() {
        try {
            super.dismiss();
        } catch (IllegalStateException ignored) {
        }
    }

    protected abstract BD getViewBinding();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireActivity(), R.style.Theme_WebDefender_Dialog3);
        binding = getViewBinding();
        dialogBuilder.setView(binding.getRoot());
        initView();
        initControl();
        return dialogBuilder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (builder == null) {
            dismiss();
            return;
        }

        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            WindowManager.LayoutParams windowParams = window.getAttributes();
            windowParams.dimAmount = 0.8f;
            windowParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            windowParams.gravity = Gravity.CENTER;
            window.setAttributes(windowParams);
            dialog.setCancelable(builder.cancelable);
            dialog.setCanceledOnTouchOutside(builder.canOntouchOutside);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (builder == null) {
            dismiss();
            return;
        }
    }

    protected void initView() {

        if (builder == null) {
            return;
        }

        if (!TextUtils.isEmpty(builder.title) && getTitle() != null) {
            getTitle().setText(builder.title);
        }

        if (!TextUtils.isEmpty(builder.positiveButton) && getPositiveButton() != null) {
            getPositiveButton().setText(builder.positiveButton);
            getPositiveButton().setOnClickListener(v -> handleClickPositiveButton(new HashMap<>()));
        }

        if (!TextUtils.isEmpty(builder.negativeButton) && getNegativeButton() != null) {
            getNegativeButton().setText(builder.negativeButton);
            getNegativeButton().setOnClickListener(v -> handleClickNegativeButton());
        }

    }

    protected abstract void initControl();

    protected TextView getPositiveButton() {
        return null;
    }

    protected TextView getNegativeButton() {
        return null;
    }

    protected TextView getTitle() {
        return null;
    }

    @SuppressWarnings("unchecked")
    protected void handleClickNegativeButton() {
        if (builder.negativeButtonListener != null) {
            builder.negativeButtonListener.onNegativeButtonListener((T) this);
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleClickPositiveButton(HashMap<String, Object> datas) {
        if (builder.positiveButtonListener != null) {
            builder.positiveButtonListener.onPositiveButtonListener((T) this, datas);
        }
    }

}
