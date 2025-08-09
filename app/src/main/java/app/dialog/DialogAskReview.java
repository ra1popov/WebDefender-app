package app.dialog;

import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.annotation.Keep;

import app.databinding.DialogAskReviewBinding;

@Keep
public class DialogAskReview extends BaseDialog2<DialogAskReview, DialogAskReviewBinding, DialogAskReview.ExtendBuilder> {

    public DialogAskReview() {
        super();
    }

    public DialogAskReview(ExtendBuilder builder) {
        super(builder);
    }

    @Override
    protected DialogAskReviewBinding getViewBinding() {
        return DialogAskReviewBinding.inflate(LayoutInflater.from(requireContext()));
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
    protected void initControl() {

    }

    public static class ExtendBuilder extends BuilderDialog<DialogAskReview> {

        @Override
        public DialogAskReview build() {
            return new DialogAskReview(this);
        }

    }

}
