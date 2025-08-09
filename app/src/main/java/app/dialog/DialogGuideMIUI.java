package app.dialog;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Keep;

import app.analytics.MyAnalyticsSDK;
import app.databinding.DialogGuideMiuiBinding;
import app.util.Toolbox;

@Keep
public class DialogGuideMIUI extends BaseDialog<DialogGuideMIUI, DialogGuideMiuiBinding, DialogGuideMIUI.ExtendBuilder> {

    public DialogGuideMIUI() {
        super();
    }

    public DialogGuideMIUI(ExtendBuilder builder) {
        super(builder);
    }

    @Override
    protected DialogGuideMiuiBinding getViewBinding() {
        return DialogGuideMiuiBinding.inflate(LayoutInflater.from(requireContext()));
    }

    @Override
    protected void initView() {
        super.initView();

        if (isMIUIPrior12()) {
            binding.svContentMiui12.setVisibility(View.GONE);
            binding.svContentMiuiPrior12.setVisibility(View.VISIBLE);
        } else {
            binding.svContentMiuiPrior12.setVisibility(View.GONE);
            binding.svContentMiui12.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void initControl() {

    }

    @Override
    protected TextView getPositiveButton() {
        return binding.tvPositive;
    }

    @Override
    protected LinearLayout getContainer() {
        return binding.container;
    }

    public static class ExtendBuilder extends BuilderDialog<DialogGuideMIUI> {

        @Override
        public DialogGuideMIUI build() {
            return new DialogGuideMIUI(this);
        }

    }

    private boolean isMIUIPrior12() {
        String _version1 = "";
        String _version2;
        String _version3;

        int version1 = 0;
        int version2 = 0;

        if (Build.VERSION.INCREMENTAL != null) {
            _version1 = Build.VERSION.INCREMENTAL;
            int dotIndex = _version1.indexOf('.');
            if (dotIndex != -1) {
                String _tmp = _version1.substring(0, dotIndex);
                _tmp = _tmp.replaceAll("[^0-9]", "");
                try {
                    version1 = Integer.parseInt(_tmp);
                } catch (Exception ignored) {
                }
            }
        }

        _version2 = Toolbox.getSystemProperty("ro.miui.ui.version.code");
        if (_version2 != null) {
            try {
                version2 = Integer.parseInt(_version2);
            } catch (Exception ignored) {
            }
        }

        _version3 = Toolbox.getSystemProperty("ro.miui.ui.version.name");

        MyAnalyticsSDK.log("MIUI version " + _version1 + " : " + _version2 + " : " + _version3);

        if (version2 < 10) {
            return true;
        }

        return false;
    }

}
