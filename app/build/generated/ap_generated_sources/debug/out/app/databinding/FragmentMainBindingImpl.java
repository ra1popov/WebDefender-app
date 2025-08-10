package app.databinding;
import app.R;
import app.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class FragmentMainBindingImpl extends FragmentMainBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = null;
    }
    // views
    @NonNull
    private final android.widget.RelativeLayout mboundView0;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers
    private androidx.databinding.InverseBindingListener powerViewpowerAttrChanged = new androidx.databinding.InverseBindingListener() {
        @Override
        public void onChange() {
            // Inverse of viewModel.power.getValue()
            //         is viewModel.power.setValue((java.lang.Boolean) callbackArg_0)
            java.lang.Boolean callbackArg_0 = app.ui.main.MainFragment.isPower(powerView);
            // localize variables for thread safety
            // viewModel.power
            androidx.lifecycle.MutableLiveData<java.lang.Boolean> viewModelPower = null;
            // viewModel.power.getValue()
            java.lang.Boolean viewModelPowerGetValue = null;
            // viewModel.power != null
            boolean viewModelPowerJavaLangObjectNull = false;
            // viewModel
            app.ui.main.MainViewModel viewModel = mViewModel;
            // viewModel != null
            boolean viewModelJavaLangObjectNull = false;



            viewModelJavaLangObjectNull = (viewModel) != (null);
            if (viewModelJavaLangObjectNull) {


                viewModelPower = viewModel.power;

                viewModelPowerJavaLangObjectNull = (viewModelPower) != (null);
                if (viewModelPowerJavaLangObjectNull) {




                    viewModelPower.setValue(((java.lang.Boolean) (callbackArg_0)));
                }
            }
        }
    };

    public FragmentMainBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 3, sIncludes, sViewsWithIds));
    }
    private FragmentMainBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 3
            , (app.ui.main.widget.PowerView) bindings[1]
            , (app.ui.main.widget.StatView) bindings[2]
            );
        this.mboundView0 = (android.widget.RelativeLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.powerView.setTag(null);
        this.statView.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x20L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
        if (BR.handler == variableId) {
            setHandler((app.ui.main.MainHandler) variable);
        }
        else if (BR.viewModel == variableId) {
            setViewModel((app.ui.main.MainViewModel) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setHandler(@Nullable app.ui.main.MainHandler Handler) {
        this.mHandler = Handler;
    }
    public void setViewModel(@Nullable app.ui.main.MainViewModel ViewModel) {
        this.mViewModel = ViewModel;
        synchronized(this) {
            mDirtyFlags |= 0x10L;
        }
        notifyPropertyChanged(BR.viewModel);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
            case 0 :
                return onChangeViewModelPower((androidx.lifecycle.MutableLiveData<java.lang.Boolean>) object, fieldId);
            case 1 :
                return onChangeViewModelStat((androidx.lifecycle.MutableLiveData<app.preferences.model.Stat>) object, fieldId);
            case 2 :
                return onChangeViewModelAd((androidx.lifecycle.MutableLiveData<java.lang.Boolean>) object, fieldId);
        }
        return false;
    }
    private boolean onChangeViewModelPower(androidx.lifecycle.MutableLiveData<java.lang.Boolean> ViewModelPower, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x1L;
            }
            return true;
        }
        return false;
    }
    private boolean onChangeViewModelStat(androidx.lifecycle.MutableLiveData<app.preferences.model.Stat> ViewModelStat, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x2L;
            }
            return true;
        }
        return false;
    }
    private boolean onChangeViewModelAd(androidx.lifecycle.MutableLiveData<java.lang.Boolean> ViewModelAd, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x4L;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        boolean androidxDatabindingViewDataBindingSafeUnboxViewModelAdGetValue = false;
        java.lang.Boolean viewModelAdGetValue = null;
        androidx.lifecycle.MutableLiveData<java.lang.Boolean> viewModelPower = null;
        androidx.lifecycle.MutableLiveData<app.preferences.model.Stat> viewModelStat = null;
        app.preferences.model.Stat viewModelStatGetValue = null;
        java.lang.Boolean viewModelPowerGetValue = null;
        app.ui.main.MainViewModel viewModel = mViewModel;
        boolean androidxDatabindingViewDataBindingSafeUnboxViewModelPowerGetValue = false;
        androidx.lifecycle.MutableLiveData<java.lang.Boolean> viewModelAd = null;

        if ((dirtyFlags & 0x37L) != 0) {


            if ((dirtyFlags & 0x31L) != 0) {

                    if (viewModel != null) {
                        // read viewModel.power
                        viewModelPower = viewModel.power;
                    }
                    updateLiveDataRegistration(0, viewModelPower);


                    if (viewModelPower != null) {
                        // read viewModel.power.getValue()
                        viewModelPowerGetValue = viewModelPower.getValue();
                    }


                    // read androidx.databinding.ViewDataBinding.safeUnbox(viewModel.power.getValue())
                    androidxDatabindingViewDataBindingSafeUnboxViewModelPowerGetValue = androidx.databinding.ViewDataBinding.safeUnbox(viewModelPowerGetValue);
            }
            if ((dirtyFlags & 0x32L) != 0) {

                    if (viewModel != null) {
                        // read viewModel.stat
                        viewModelStat = viewModel.stat;
                    }
                    updateLiveDataRegistration(1, viewModelStat);


                    if (viewModelStat != null) {
                        // read viewModel.stat.getValue()
                        viewModelStatGetValue = viewModelStat.getValue();
                    }
            }
            if ((dirtyFlags & 0x34L) != 0) {

                    if (viewModel != null) {
                        // read viewModel.ad
                        viewModelAd = viewModel.ad;
                    }
                    updateLiveDataRegistration(2, viewModelAd);


                    if (viewModelAd != null) {
                        // read viewModel.ad.getValue()
                        viewModelAdGetValue = viewModelAd.getValue();
                    }


                    // read androidx.databinding.ViewDataBinding.safeUnbox(viewModel.ad.getValue())
                    androidxDatabindingViewDataBindingSafeUnboxViewModelAdGetValue = androidx.databinding.ViewDataBinding.safeUnbox(viewModelAdGetValue);
            }
        }
        // batch finished
        if ((dirtyFlags & 0x31L) != 0) {
            // api target 1

            app.ui.main.MainFragment.setPower(this.powerView, androidxDatabindingViewDataBindingSafeUnboxViewModelPowerGetValue);
            app.ui.main.MainFragment.setStatPower(this.statView, androidxDatabindingViewDataBindingSafeUnboxViewModelPowerGetValue);
        }
        if ((dirtyFlags & 0x20L) != 0) {
            // api target 1

            app.ui.main.MainFragment.setOnPowerListener(this.powerView, powerViewpowerAttrChanged);
        }
        if ((dirtyFlags & 0x32L) != 0) {
            // api target 1

            app.ui.main.MainFragment.setStatData(this.statView, viewModelStatGetValue);
        }
        if ((dirtyFlags & 0x34L) != 0) {
            // api target 1

            app.ui.main.MainFragment.setStatShowAd(this.statView, androidxDatabindingViewDataBindingSafeUnboxViewModelAdGetValue);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): viewModel.power
        flag 1 (0x2L): viewModel.stat
        flag 2 (0x3L): viewModel.ad
        flag 3 (0x4L): handler
        flag 4 (0x5L): viewModel
        flag 5 (0x6L): null
    flag mapping end*/
    //end
}