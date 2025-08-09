package app.ui;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import app.util.Callback;
import app.widget.ToolbarView;

public abstract class BaseFragment<B extends ViewDataBinding> extends Fragment {

    private final ActivityResultLauncher<Intent> askIntentResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        Boolean _result = Boolean.FALSE;
        if (result.getResultCode() == RESULT_OK) {
            _result = Boolean.TRUE;
        }
        callBooleanListener(_result);
    });

    protected Callback<Boolean> callbackBoolean;
    protected B binding;

    protected void callBooleanListener(Boolean result) {
        try {
            this.callbackBoolean.setResult(result);
            this.callbackBoolean.call();
        } catch (Exception ignored) {
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = getViewBinding(inflater, container);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
        initControl();
    }

    protected abstract void initView();

    protected abstract void initControl();

    protected abstract B getViewBinding(LayoutInflater inflater, ViewGroup container);

    @SuppressWarnings("rawtypes")
    public BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    @NonNull
    protected ToolbarView getToolbar() {
        return getBaseActivity().getToolbar();
    }

    public void toast(String content) {
        BaseActivity<?> activity = getBaseActivity();
        if (activity != null) {
            activity.toast(content);
        }
    }

    public void navigate(int id) {
        navigate(id, null);
    }

    public void navigate(int id, Bundle bundle) {
        try {
            NavHostFragment.findNavController(BaseFragment.this).navigate(
                    id,
                    bundle,
                    null,
                    null);
        } catch (Exception ignored) {
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////

    public void askIntent(@NonNull Intent intent, Callback<Boolean> callback) {
        try {
            this.callbackBoolean = callback;
            this.askIntentResult.launch(intent);
        } catch (Exception ignored) {
        }
    }

}
