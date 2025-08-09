package app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;

import app.R;

public class LoadingRecyclerView extends RecyclerView {

    private final AdapterDataObserver adapterDataObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            Adapter<?> adapter = getAdapter();
            if (adapter != null && emptyView != null) {
                if (adapter.getItemCount() == 0) {
                    emptyView.setVisibility(View.VISIBLE);

                    Glide.with(context)
                            .asGif()
                            .load(R.drawable.ic_preloader)
                            .priority(Priority.IMMEDIATE) // Setting the highest download priority.
                            .into(emptyView);

                    setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    emptyView.setImageDrawable(null);
                    setVisibility(View.VISIBLE);
                }
            }
        }
    };

    private Context context;
    private ImageView emptyView;


    public LoadingRecyclerView(Context context) {
        super(context);
    }

    public LoadingRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoadingRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);

        if (adapter != null) {
            adapter.registerAdapterDataObserver(adapterDataObserver);
        }

        adapterDataObserver.onChanged();
    }

    public void setEmptyView(ImageView emptyView) {
        this.emptyView = emptyView;

        Glide.with(this)
                .asGif()
                .load(R.drawable.ic_preloader)
                .priority(Priority.IMMEDIATE) // Setting the highest download priority.
                .into(emptyView);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

}
