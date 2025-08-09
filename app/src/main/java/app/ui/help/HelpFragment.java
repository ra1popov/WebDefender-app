package app.ui.help;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import app.R;
import app.databinding.FragmentHelpBinding;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.internal.Settings;
import app.ui.BaseActivity;
import app.ui.BaseFragment;
import app.util.SafeOnClickListener;
import app.util.Toolbox;
import dev.doubledot.doki.ui.DokiActivity;

public class HelpFragment extends BaseFragment<FragmentHelpBinding> implements HelpHandler {

    private final SafeOnClickListener onClickLocker = new SafeOnClickListener(); // lock double click
    private HelpViewModel helpViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        helpViewModel = new ViewModelProvider(requireActivity()).get(HelpViewModel.class);
    }

    @Override
    protected void initView() {
        getToolbar().setTitle(R.string.menu_help);

        if (Preferences.get(Settings.PREF_APP_ADBLOCK)) {
            binding.llGuide.setVisibility(View.VISIBLE);
        } else {
            binding.llGuide.setVisibility(View.GONE);
        }

    }

    @Override
    protected void initControl() {
        getToolbar().setOnRightClickListener(null);

    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected FragmentHelpBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        FragmentHelpBinding binding = FragmentHelpBinding.inflate(inflater, container, false);
        binding.setViewModel(helpViewModel);
        binding.setHandler(this);
        binding.setLifecycleOwner(requireActivity());
        return binding;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onClickGuide(View view) {
        onClickLocker.onClick(view);

        ApplicationDependencies.getGuideHelper().showGuide((BaseActivity<?>) requireActivity());
    }

    @Override
    public void onClickHelp(View view) {
        onClickLocker.onClick(view);

        DokiActivity.Companion.start(requireContext());
    }

    @Override
    public void onClickSupport(View view) {
        onClickLocker.onClick(view);

        Toolbox.feedback(requireContext());
    }

}
