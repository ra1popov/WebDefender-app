package app.dialog;

import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Keep;

import app.databinding.DialogGuideInfoBinding;

@Keep
public class DialogGuideInfo extends BaseDialog<DialogGuideInfo, DialogGuideInfoBinding, DialogGuideInfo.ExtendBuilder> {

    public DialogGuideInfo() {
        super();
    }

    public DialogGuideInfo(ExtendBuilder builder) {
        super(builder);
    }

    @Override
    protected DialogGuideInfoBinding getViewBinding() {
        return DialogGuideInfoBinding.inflate(LayoutInflater.from(requireContext()));
    }

    @Override
    protected void initView() {
        super.initView();

    }

    @Override
    protected void initControl() {

    }

    @Override
    protected TextView getPositiveButton() {
        return binding.tvPositive;
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
    protected LinearLayout getContainer() {
        return binding.container;
    }

    public static class ExtendBuilder extends BuilderDialog<DialogGuideInfo> {

        @Override
        public DialogGuideInfo build() {
            return new DialogGuideInfo(this);
        }

    }

}
