package app.dialog;

import android.view.LayoutInflater;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;

import java.util.Objects;

import app.ad.Ad;
import app.ad.AdRepository;
import app.databinding.DialogAdShowBinding;
import app.util.Toolbox;

@Keep
public class DialogAdShow extends BaseDialog3<DialogAdShow, DialogAdShowBinding, DialogAdShow.ExtendBuilder> {

    private Ad ad;

    public DialogAdShow() {
        super();
    }

    public DialogAdShow(ExtendBuilder builder) {
        super(builder);
    }

    @Override
    protected DialogAdShowBinding getViewBinding() {
        return DialogAdShowBinding.inflate(LayoutInflater.from(requireContext()));
    }

    @Override
    protected void initView() {
        super.initView();

    }

    @Override
    protected void initControl() {

    }

    public void show(@NonNull FragmentManager manager, @Nullable String tag, @NonNull Ad ad) {
        this.ad = ad;

        String currentLanguage = Toolbox.getCurrentLanguage();
        ad.images.forEach((language, image) -> {
            if (Objects.equals(currentLanguage, language)) {
                ad.image = image;
            }
        });

        show(manager, tag);
    }

    @Override
    public void onStart() {
        super.onStart();

        binding.llClose.setOnClickListener(view -> dismiss());
        binding.ivAd.setOnClickListener(view -> Toolbox.openURL(requireContext(), ad.url));

        Glide.with(this)
                .load(AdRepository.getAdPreview(ad))
                .into(binding.ivAd);
    }

    public static class ExtendBuilder extends BuilderDialog<DialogAdShow> {

        @Override
        public DialogAdShow build() {
            return new DialogAdShow(this);
        }

    }

}
