package app.databinding;
import app.R;
import app.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class FragmentProxyBindingImpl extends FragmentProxyBinding  {

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

    public FragmentProxyBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 3, sIncludes, sViewsWithIds));
    }
    private FragmentProxyBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 2
            , (androidx.recyclerview.widget.RecyclerView) bindings[1]
            , (android.widget.TextView) bindings[2]
            );
        this.mboundView0 = (android.widget.RelativeLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.rvProxy.setTag(null);
        this.tvProxyBlocked.setTag(null);
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
            setHandler((app.ui.proxy.ProxyHandler) variable);
        }
        else if (BR.viewModel == variableId) {
            setViewModel((app.ui.proxy.ProxyViewModel) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setHandler(@Nullable app.ui.proxy.ProxyHandler Handler) {
        this.mHandler = Handler;
    }
    public void setViewModel(@Nullable app.ui.proxy.ProxyViewModel ViewModel) {
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
                return onChangeViewModelList((androidx.lifecycle.MutableLiveData<java.util.List<app.ui.proxy.Proxy>>) object, fieldId);
            case 1 :
                return onChangeViewModelShow((androidx.lifecycle.MutableLiveData<java.lang.Boolean>) object, fieldId);
        }
        return false;
    }
    private boolean onChangeViewModelList(androidx.lifecycle.MutableLiveData<java.util.List<app.ui.proxy.Proxy>> ViewModelList, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x1L;
            }
            return true;
        }
        return false;
    }
    private boolean onChangeViewModelShow(androidx.lifecycle.MutableLiveData<java.lang.Boolean> ViewModelShow, int fieldId) {
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
        androidx.lifecycle.MutableLiveData<java.util.List<app.ui.proxy.Proxy>> viewModelList = null;
        java.lang.Boolean viewModelShowGetValue = null;
        java.util.List<app.ui.proxy.Proxy> viewModelListGetValue = null;
        boolean androidxDatabindingViewDataBindingSafeUnboxViewModelShowGetValue = false;
        int viewModelShowViewINVISIBLEViewVISIBLE = 0;
        int viewModelShowViewVISIBLEViewINVISIBLE = 0;
        app.ui.proxy.ProxyViewModel viewModel = mViewModel;
        androidx.lifecycle.MutableLiveData<java.lang.Boolean> viewModelShow = null;

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
                        // read viewModel.show
                        viewModelShow = viewModel.show;
                    }
                    updateLiveDataRegistration(1, viewModelShow);


                    if (viewModelShow != null) {
                        // read viewModel.show.getValue()
                        viewModelShowGetValue = viewModelShow.getValue();
                    }


                    // read androidx.databinding.ViewDataBinding.safeUnbox(viewModel.show.getValue())
                    androidxDatabindingViewDataBindingSafeUnboxViewModelShowGetValue = androidx.databinding.ViewDataBinding.safeUnbox(viewModelShowGetValue);
                if((dirtyFlags & 0x1aL) != 0) {
                    if(androidxDatabindingViewDataBindingSafeUnboxViewModelShowGetValue) {
                            dirtyFlags |= 0x40L;
                            dirtyFlags |= 0x100L;
                    }
                    else {
                            dirtyFlags |= 0x20L;
                            dirtyFlags |= 0x80L;
                    }
                }


                    // read androidx.databinding.ViewDataBinding.safeUnbox(viewModel.show.getValue()) ? View.INVISIBLE : View.VISIBLE
                    viewModelShowViewINVISIBLEViewVISIBLE = ((androidxDatabindingViewDataBindingSafeUnboxViewModelShowGetValue) ? (android.view.View.INVISIBLE) : (android.view.View.VISIBLE));
                    // read androidx.databinding.ViewDataBinding.safeUnbox(viewModel.show.getValue()) ? View.VISIBLE : View.INVISIBLE
                    viewModelShowViewVISIBLEViewINVISIBLE = ((androidxDatabindingViewDataBindingSafeUnboxViewModelShowGetValue) ? (android.view.View.VISIBLE) : (android.view.View.INVISIBLE));
            }
        }
        // batch finished
        if ((dirtyFlags & 0x1aL) != 0) {
            // api target 1

            this.rvProxy.setVisibility(viewModelShowViewVISIBLEViewINVISIBLE);
            this.tvProxyBlocked.setVisibility(viewModelShowViewINVISIBLEViewVISIBLE);
        }
        if ((dirtyFlags & 0x19L) != 0) {
            // api target 1

            app.ui.proxy.ProxyFragment.setProxy(this.rvProxy, viewModelListGetValue);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): viewModel.list
        flag 1 (0x2L): viewModel.show
        flag 2 (0x3L): handler
        flag 3 (0x4L): viewModel
        flag 4 (0x5L): null
        flag 5 (0x6L): androidx.databinding.ViewDataBinding.safeUnbox(viewModel.show.getValue()) ? View.INVISIBLE : View.VISIBLE
        flag 6 (0x7L): androidx.databinding.ViewDataBinding.safeUnbox(viewModel.show.getValue()) ? View.INVISIBLE : View.VISIBLE
        flag 7 (0x8L): androidx.databinding.ViewDataBinding.safeUnbox(viewModel.show.getValue()) ? View.VISIBLE : View.INVISIBLE
        flag 8 (0x9L): androidx.databinding.ViewDataBinding.safeUnbox(viewModel.show.getValue()) ? View.VISIBLE : View.INVISIBLE
    flag mapping end*/
    //end
}