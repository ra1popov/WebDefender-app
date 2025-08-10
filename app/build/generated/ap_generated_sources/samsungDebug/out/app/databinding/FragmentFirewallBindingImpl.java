package app.databinding;
import app.R;
import app.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class FragmentFirewallBindingImpl extends FragmentFirewallBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.sv_search, 2);
        sViewsWithIds.put(R.id.tv_tooltip, 3);
        sViewsWithIds.put(R.id.iv_firewall_empty, 4);
    }
    // views
    @NonNull
    private final app.widget.BlockableLinearLayout mboundView0;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public FragmentFirewallBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 5, sIncludes, sViewsWithIds));
    }
    private FragmentFirewallBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 2
            , (android.widget.ImageView) bindings[4]
            , (app.widget.LoadingRecyclerView) bindings[1]
            , (androidx.appcompat.widget.SearchView) bindings[2]
            , (android.widget.TextView) bindings[3]
            );
        this.mboundView0 = (app.widget.BlockableLinearLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.rvFirewall.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x10L;
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
            setHandler((app.ui.firewall.FirewallHandler) variable);
        }
        else if (BR.viewModel == variableId) {
            setViewModel((app.ui.firewall.FirewallViewModel) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setHandler(@Nullable app.ui.firewall.FirewallHandler Handler) {
        this.mHandler = Handler;
    }
    public void setViewModel(@Nullable app.ui.firewall.FirewallViewModel ViewModel) {
        this.mViewModel = ViewModel;
        synchronized(this) {
            mDirtyFlags |= 0x8L;
        }
        notifyPropertyChanged(BR.viewModel);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
            case 0 :
                return onChangeViewModelList((androidx.lifecycle.MutableLiveData<java.util.List<app.application.ApplicationInfo>>) object, fieldId);
            case 1 :
                return onChangeViewModelBrowsers((androidx.lifecycle.MutableLiveData<java.lang.Boolean>) object, fieldId);
        }
        return false;
    }
    private boolean onChangeViewModelList(androidx.lifecycle.MutableLiveData<java.util.List<app.application.ApplicationInfo>> ViewModelList, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x1L;
            }
            return true;
        }
        return false;
    }
    private boolean onChangeViewModelBrowsers(androidx.lifecycle.MutableLiveData<java.lang.Boolean> ViewModelBrowsers, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x2L;
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
        androidx.lifecycle.MutableLiveData<java.util.List<app.application.ApplicationInfo>> viewModelList = null;
        java.lang.Boolean viewModelBrowsersGetValue = null;
        boolean androidxDatabindingViewDataBindingSafeUnboxViewModelBrowsers = false;
        java.util.List<app.application.ApplicationInfo> viewModelListGetValue = null;
        androidx.lifecycle.MutableLiveData<java.lang.Boolean> viewModelBrowsers = null;
        boolean androidxDatabindingViewDataBindingSafeUnboxViewModelBrowsersGetValue = false;
        boolean ViewModelBrowsers1 = false;
        app.ui.firewall.FirewallViewModel viewModel = mViewModel;

        if ((dirtyFlags & 0x1bL) != 0) {


            if ((dirtyFlags & 0x19L) != 0) {

                    if (viewModel != null) {
                        // read viewModel.list
                        viewModelList = viewModel.list;
                    }
                    updateLiveDataRegistration(0, viewModelList);


                    if (viewModelList != null) {
                        // read viewModel.list.getValue()
                        viewModelListGetValue = viewModelList.getValue();
                    }
            }
            if ((dirtyFlags & 0x1aL) != 0) {

                    if (viewModel != null) {
                        // read viewModel.browsers
                        viewModelBrowsers = viewModel.browsers;
                    }
                    updateLiveDataRegistration(1, viewModelBrowsers);


                    if (viewModelBrowsers != null) {
                        // read viewModel.browsers.getValue()
                        viewModelBrowsersGetValue = viewModelBrowsers.getValue();
                    }


                    // read androidx.databinding.ViewDataBinding.safeUnbox(viewModel.browsers.getValue())
                    androidxDatabindingViewDataBindingSafeUnboxViewModelBrowsersGetValue = androidx.databinding.ViewDataBinding.safeUnbox(viewModelBrowsersGetValue);


                    // read !androidx.databinding.ViewDataBinding.safeUnbox(viewModel.browsers.getValue())
                    ViewModelBrowsers1 = !androidxDatabindingViewDataBindingSafeUnboxViewModelBrowsersGetValue;


                    // read androidx.databinding.ViewDataBinding.safeUnbox(!androidx.databinding.ViewDataBinding.safeUnbox(viewModel.browsers.getValue()))
                    androidxDatabindingViewDataBindingSafeUnboxViewModelBrowsers = androidx.databinding.ViewDataBinding.safeUnbox(ViewModelBrowsers1);
            }
        }
        // batch finished
        if ((dirtyFlags & 0x1aL) != 0) {
            // api target 1

            this.mboundView0.setActive(androidxDatabindingViewDataBindingSafeUnboxViewModelBrowsers);
            this.mboundView0.setClickable(androidxDatabindingViewDataBindingSafeUnboxViewModelBrowsers);
        }
        if ((dirtyFlags & 0x19L) != 0) {
            // api target 1

            app.ui.firewall.FirewallFragment.setFirewall(this.rvFirewall, viewModelListGetValue);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): viewModel.list
        flag 1 (0x2L): viewModel.browsers
        flag 2 (0x3L): handler
        flag 3 (0x4L): viewModel
        flag 4 (0x5L): null
    flag mapping end*/
    //end
}