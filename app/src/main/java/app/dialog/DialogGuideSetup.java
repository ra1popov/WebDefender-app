package app.dialog;

import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Keep;

import app.databinding.DialogGuideSetupBinding;

@Keep
public class DialogGuideSetup extends BaseDialog<DialogGuideSetup, DialogGuideSetupBinding, DialogGuideSetup.ExtendBuilder> {

    public DialogGuideSetup() {
        super();
    }

    public DialogGuideSetup(ExtendBuilder builder) {
        super(builder);
    }

    @Override
    protected DialogGuideSetupBinding getViewBinding() {
        return DialogGuideSetupBinding.inflate(LayoutInflater.from(requireContext()));
    }

    @Override
    protected void initView() {
        super.initView();

    }

    @Override
    protected TextView getPositiveButton() {
        return binding.tvPositive;
    }

    @Override
    protected TextView getNegativeButton() {
        return binding.tvNegative;
    }

    @Override
    protected TextView getTitle() {
        return binding.tvTitle;
    }

    @Override
    protected TextView getContent() {
        return binding.tvContent;
    }

    @Override
    protected void initControl() {

    }

    @Override
    protected LinearLayout getContainer() {
        return binding.container;
    }

    public static class ExtendBuilder extends BuilderDialog<DialogGuideSetup> {

        @Override
        public DialogGuideSetup build() {
            return new DialogGuideSetup(this);
        }

    }

}
