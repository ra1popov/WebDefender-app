package app;

import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import androidx.databinding.DataBinderMapper;
import androidx.databinding.DataBindingComponent;
import androidx.databinding.ViewDataBinding;
import app.databinding.FragmentFirewallBindingImpl;
import app.databinding.FragmentHelpBindingImpl;
import app.databinding.FragmentMainBindingImpl;
import app.databinding.FragmentProxyBindingImpl;
import app.databinding.FragmentStatlogBindingImpl;
import app.databinding.FragmentWhitelistBindingImpl;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataBinderMapperImpl extends DataBinderMapper {
  private static final int LAYOUT_FRAGMENTFIREWALL = 1;

  private static final int LAYOUT_FRAGMENTHELP = 2;

  private static final int LAYOUT_FRAGMENTMAIN = 3;

  private static final int LAYOUT_FRAGMENTPROXY = 4;

  private static final int LAYOUT_FRAGMENTSTATLOG = 5;

  private static final int LAYOUT_FRAGMENTWHITELIST = 6;

  private static final SparseIntArray INTERNAL_LAYOUT_ID_LOOKUP = new SparseIntArray(6);

  static {
    INTERNAL_LAYOUT_ID_LOOKUP.put(app.R.layout.fragment_firewall, LAYOUT_FRAGMENTFIREWALL);
    INTERNAL_LAYOUT_ID_LOOKUP.put(app.R.layout.fragment_help, LAYOUT_FRAGMENTHELP);
    INTERNAL_LAYOUT_ID_LOOKUP.put(app.R.layout.fragment_main, LAYOUT_FRAGMENTMAIN);
    INTERNAL_LAYOUT_ID_LOOKUP.put(app.R.layout.fragment_proxy, LAYOUT_FRAGMENTPROXY);
    INTERNAL_LAYOUT_ID_LOOKUP.put(app.R.layout.fragment_statlog, LAYOUT_FRAGMENTSTATLOG);
    INTERNAL_LAYOUT_ID_LOOKUP.put(app.R.layout.fragment_whitelist, LAYOUT_FRAGMENTWHITELIST);
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View view, int layoutId) {
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = view.getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
        case  LAYOUT_FRAGMENTFIREWALL: {
          if ("layout/fragment_firewall_0".equals(tag)) {
            return new FragmentFirewallBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for fragment_firewall is invalid. Received: " + tag);
        }
        case  LAYOUT_FRAGMENTHELP: {
          if ("layout/fragment_help_0".equals(tag)) {
            return new FragmentHelpBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for fragment_help is invalid. Received: " + tag);
        }
        case  LAYOUT_FRAGMENTMAIN: {
          if ("layout/fragment_main_0".equals(tag)) {
            return new FragmentMainBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for fragment_main is invalid. Received: " + tag);
        }
        case  LAYOUT_FRAGMENTPROXY: {
          if ("layout/fragment_proxy_0".equals(tag)) {
            return new FragmentProxyBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for fragment_proxy is invalid. Received: " + tag);
        }
        case  LAYOUT_FRAGMENTSTATLOG: {
          if ("layout/fragment_statlog_0".equals(tag)) {
            return new FragmentStatlogBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for fragment_statlog is invalid. Received: " + tag);
        }
        case  LAYOUT_FRAGMENTWHITELIST: {
          if ("layout/fragment_whitelist_0".equals(tag)) {
            return new FragmentWhitelistBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for fragment_whitelist is invalid. Received: " + tag);
        }
      }
    }
    return null;
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View[] views, int layoutId) {
    if(views == null || views.length == 0) {
      return null;
    }
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = views[0].getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
      }
    }
    return null;
  }

  @Override
  public int getLayoutId(String tag) {
    if (tag == null) {
      return 0;
    }
    Integer tmpVal = InnerLayoutIdLookup.sKeys.get(tag);
    return tmpVal == null ? 0 : tmpVal;
  }

  @Override
  public String convertBrIdToString(int localId) {
    String tmpVal = InnerBrLookup.sKeys.get(localId);
    return tmpVal;
  }

  @Override
  public List<DataBinderMapper> collectDependencies() {
    ArrayList<DataBinderMapper> result = new ArrayList<DataBinderMapper>(1);
    result.add(new androidx.databinding.library.baseAdapters.DataBinderMapperImpl());
    return result;
  }

  private static class InnerBrLookup {
    static final SparseArray<String> sKeys = new SparseArray<String>(3);

    static {
      sKeys.put(0, "_all");
      sKeys.put(1, "handler");
      sKeys.put(2, "viewModel");
    }
  }

  private static class InnerLayoutIdLookup {
    static final HashMap<String, Integer> sKeys = new HashMap<String, Integer>(6);

    static {
      sKeys.put("layout/fragment_firewall_0", app.R.layout.fragment_firewall);
      sKeys.put("layout/fragment_help_0", app.R.layout.fragment_help);
      sKeys.put("layout/fragment_main_0", app.R.layout.fragment_main);
      sKeys.put("layout/fragment_proxy_0", app.R.layout.fragment_proxy);
      sKeys.put("layout/fragment_statlog_0", app.R.layout.fragment_statlog);
      sKeys.put("layout/fragment_whitelist_0", app.R.layout.fragment_whitelist);
    }
  }
}
