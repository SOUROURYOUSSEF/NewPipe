package org.schabi.newpipe.fragments.list;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.fragments.BaseStateFragment;
import org.schabi.newpipe.fragments.OnScrollBelowItemsListener;
import org.schabi.newpipe.info_list.InfoItemBuilder;
import org.schabi.newpipe.info_list.InfoListAdapter;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.StateSaver;

import java.util.List;
import java.util.Queue;

import static org.schabi.newpipe.util.AnimationUtils.animateView;

public abstract class BaseListFragment<I, N> extends BaseStateFragment<I> implements ListViewContract<I, N>, StateSaver.WriteRead {

    /*//////////////////////////////////////////////////////////////////////////
    // Views
    //////////////////////////////////////////////////////////////////////////*/

    protected InfoListAdapter infoListAdapter;
    protected RecyclerView itemsList;

    /*//////////////////////////////////////////////////////////////////////////
    // LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        infoListAdapter = new InfoListAdapter(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        StateSaver.onDestroy(savedState);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // State Saving
    //////////////////////////////////////////////////////////////////////////*/

    protected StateSaver.SavedState savedState;

    @Override
    public String generateSuffix() {
        // Naive solution, but it's good for now (the items don't change)
        return "." + infoListAdapter.getItemsList().size() + ".list";
    }

    @Override
    public void writeTo(Queue<Object> objectsToSave) {
        objectsToSave.add(infoListAdapter.getItemsList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readFrom(@NonNull Queue<Object> savedObjects) throws Exception {
        infoListAdapter.getItemsList().clear();
        infoListAdapter.getItemsList().addAll((List<InfoItem>) savedObjects.poll());
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        savedState = StateSaver.tryToSave(activity.isChangingConfigurations(), savedState, bundle, this);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        savedState = StateSaver.tryToRestore(bundle, this);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Init
    //////////////////////////////////////////////////////////////////////////*/

    protected View getListHeader() {
        return null;
    }

    protected View getListFooter() {
        return activity.getLayoutInflater().inflate(R.layout.pignate_footer, itemsList, false);
    }

    protected RecyclerView.LayoutManager getListLayoutManager() {
        return new LinearLayoutManager(activity);
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        itemsList = rootView.findViewById(R.id.items_list);
        itemsList.setLayoutManager(getListLayoutManager());

        infoListAdapter.setFooter(getListFooter());
        infoListAdapter.setHeader(getListHeader());

        itemsList.setAdapter(infoListAdapter);
    }

    protected void onItemSelected(InfoItem selectedItem) {
        if (DEBUG) Log.d(TAG, "onItemSelected() called with: selectedItem = [" + selectedItem + "]");
    }

    @Override
    protected void initListeners() {
        super.initListeners();
        infoListAdapter.setOnStreamSelectedListener(new InfoItemBuilder.OnInfoItemSelectedListener<StreamInfoItem>() {
            @Override
            public void selected(StreamInfoItem selectedItem) {
                onItemSelected(selectedItem);
                NavigationHelper.openVideoDetailFragment(getFragmentManager(), selectedItem.service_id, selectedItem.url, selectedItem.name);
            }
        });

        infoListAdapter.setOnChannelSelectedListener(new InfoItemBuilder.OnInfoItemSelectedListener<ChannelInfoItem>() {
            @Override
            public void selected(ChannelInfoItem selectedItem) {
                onItemSelected(selectedItem);
                NavigationHelper.openChannelFragment(getFragmentManager(), selectedItem.service_id, selectedItem.url, selectedItem.name);
            }
        });

        infoListAdapter.setOnPlaylistSelectedListener(new InfoItemBuilder.OnInfoItemSelectedListener<PlaylistInfoItem>() {
            @Override
            public void selected(PlaylistInfoItem selectedItem) {
                onItemSelected(selectedItem);
                NavigationHelper.openPlaylistFragment(getFragmentManager(), selectedItem.service_id, selectedItem.url, selectedItem.name);
            }
        });

        itemsList.clearOnScrollListeners();
        itemsList.addOnScrollListener(new OnScrollBelowItemsListener() {
            @Override
            public void onScrolledDown(RecyclerView recyclerView) {
                onScrollToBottom();
            }
        });
    }

    protected void onScrollToBottom() {
        if (hasMoreItems() && !isLoading.get()) {
            loadMoreItems();
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) Log.d(TAG, "onCreateOptionsMenu() called with: menu = [" + menu + "], inflater = [" + inflater + "]");
        super.onCreateOptionsMenu(menu, inflater);
        ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayShowTitleEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Load and handle
    //////////////////////////////////////////////////////////////////////////*/

    protected abstract void loadMoreItems();

    protected abstract boolean hasMoreItems();

    /*//////////////////////////////////////////////////////////////////////////
    // Contract
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void showLoading() {
        super.showLoading();
        // animateView(itemsList, false, 400);
    }

    @Override
    public void hideLoading() {
        super.hideLoading();
        animateView(itemsList, true, 300);
    }

    @Override
    public void showError(String message, boolean showRetryButton) {
        super.showError(message, showRetryButton);
        showListFooter(false);
        animateView(itemsList, false, 200);
    }

    @Override
    public void showEmptyState() {
        super.showEmptyState();
        showListFooter(false);
    }

    @Override
    public void showListFooter(final boolean show) {
        itemsList.post(new Runnable() {
            @Override
            public void run() {
                infoListAdapter.showFooter(show);
            }
        });
    }

    @Override
    public void handleNextItems(N result) {
        isLoading.set(false);
    }
}
