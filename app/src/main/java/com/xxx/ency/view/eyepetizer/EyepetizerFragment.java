package com.xxx.ency.view.eyepetizer;

import android.content.Intent;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.xxx.ency.R;
import com.xxx.ency.base.BaseMVPFragment;
import com.xxx.ency.config.Constants;
import com.xxx.ency.config.EncyApplication;
import com.xxx.ency.contract.EyepetizerContract;
import com.xxx.ency.di.component.DaggerEyepetizerFragmentComponent;
import com.xxx.ency.di.module.EyepetizerFragmentModule;
import com.xxx.ency.model.bean.VideoBean;
import com.xxx.ency.presenter.EyepetizerPresenter;
import com.xxx.ency.view.eyepetizer.adapter.EyepetizerAdapter;

import butterknife.BindView;

/**
 * Created by xiarh on 2018/2/7.
 */

public class EyepetizerFragment extends BaseMVPFragment<EyepetizerPresenter> implements EyepetizerContract.View, SwipeRefreshLayout.OnRefreshListener, BaseQuickAdapter.RequestLoadMoreListener {

    @BindView(R.id.swiperefreshlayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerview_eyepetizer)
    RecyclerView recyclerView;

    private TextView tvHot;

    private TextView tvLike;

    private LinearLayout llMore;

    private RecyclerView recyclerViewTop;

    private EyepetizerAdapter dailyAdapter;

    private EyepetizerAdapter hotAdapter;

    private VideoBean hotVideoBean = new VideoBean();

    private int page = 1;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_eyepetizer;
    }

    @Override
    protected void initInject() {
        DaggerEyepetizerFragmentComponent
                .builder()
                .appComponent(EncyApplication.getAppComponent())
                .eyepetizerFragmentModule(new EyepetizerFragmentModule())
                .build()
                .inject(this);
    }

    @Override
    protected void initialize() {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setRefreshing(true);
        swipeRefreshLayout.setOnRefreshListener(this);

        mPresenter.getHotVideo("weekly", "256", "XXX");
        mPresenter.getDailyVideo(page, Constants.EYEPETIZER_UDID);

        View headerView = getActivity().getLayoutInflater().inflate(R.layout.header_eyepetizer, null);
        recyclerViewTop = headerView.findViewById(R.id.recyclerview_eyepetizer_top);
        tvHot = headerView.findViewById(R.id.txt_hot);
        tvLike = headerView.findViewById(R.id.txt_like);
        llMore = headerView.findViewById(R.id.layout_more);

        llMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, EyepetizerHotActivity.class);
                intent.putExtra("data", hotVideoBean);
                mContext.startActivity(intent);
            }
        });

        hotAdapter = new EyepetizerAdapter();
        recyclerViewTop.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewTop.setAdapter(hotAdapter);
        recyclerViewTop.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        swipeRefreshLayout.setEnabled(false);
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        swipeRefreshLayout.setEnabled(true);
                        break;
                }
            }
        });
        new PagerSnapHelper().attachToRecyclerView(recyclerViewTop);

        dailyAdapter = new EyepetizerAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerView.setAdapter(dailyAdapter);
        dailyAdapter.addHeaderView(headerView);
        dailyAdapter.setOnLoadMoreListener(this, recyclerView);
        dailyAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                VideoBean.ItemListBean videoBean = (VideoBean.ItemListBean) adapter.getData().get(position);
                Toast.makeText(mContext, videoBean.getData().getHeader().getDescription(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 下拉刷新
     */
    @Override
    public void onRefresh() {
        page = 1;
        mPresenter.getHotVideo("weekly", "256", "XXX");
        mPresenter.getDailyVideo(page, Constants.EYEPETIZER_UDID);
        // 这里的作用是防止下拉刷新的时候还可以上拉加载
        dailyAdapter.setEnableLoadMore(false);
    }

    /**
     * 上拉加载
     */
    @Override
    public void onLoadMoreRequested() {
        page++;
        mPresenter.getDailyVideo(page, Constants.EYEPETIZER_UDID);
        // 防止上拉加载的时候可以下拉刷新
        swipeRefreshLayout.setEnabled(false);
    }

    @Override
    public void showDailyVideoData(VideoBean dailyBean) {
        if (null != swipeRefreshLayout && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
            // 下拉刷新后可以上拉加载
            dailyAdapter.setEnableLoadMore(true);
        }
        if (null != dailyAdapter && dailyAdapter.isLoading()) {
            // 上拉加载后可以下拉刷新
            swipeRefreshLayout.setEnabled(true);
        }
        if (page == 1) {
            dailyAdapter.setNewData(dailyBean.getItemList());
        } else {
            dailyAdapter.addData(dailyBean.getItemList());
        }
        if (dailyBean.getItemList() != null) {
            dailyAdapter.loadMoreComplete();
        } else if (dailyBean.getItemList().size() == 0 || dailyBean.getItemList() == null) {
            dailyAdapter.loadMoreEnd();
        }
    }

    @Override
    public void failGetDailyData() {
        dailyAdapter.loadMoreFail();
        swipeRefreshLayout.setEnabled(true);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void showHotVideoData(VideoBean hotBean) {
        if (hotBean != null) {
            hotVideoBean = hotBean;
            hotAdapter.setNewData(hotBean.getItemList().subList(0, 5));
            tvHot.setVisibility(View.VISIBLE);
            tvLike.setVisibility(View.VISIBLE);
            llMore.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void failGetHotData() {

    }
}