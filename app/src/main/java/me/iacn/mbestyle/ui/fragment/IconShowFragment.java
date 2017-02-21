package me.iacn.mbestyle.ui.fragment;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import java.util.List;

import me.iacn.mbestyle.R;
import me.iacn.mbestyle.bean.IconBean;
import me.iacn.mbestyle.presenter.IconShowPresenter;
import me.iacn.mbestyle.ui.adapter.IconAdapter;

/**
 * Created by iAcn on 2017/2/18
 * Emali iAcn0301@foxmail.com
 */

public class IconShowFragment extends IIconFragment {

    private RecyclerView rvIcon;
    private IconShowPresenter mPresenter;
    private List<IconBean> mIcons;

    @Override
    protected int getContentView() {
        return R.layout.fragment_show_icon;
    }

    @Override
    protected void findView() {
        rvIcon = (RecyclerView) findViewById(R.id.rv_icon);
    }

    @Override
    protected void setListener() {
        rvIcon.setLayoutManager(new GridLayoutManager(getActivity(), 4));
    }

    @Override
    protected void initData() {
        mPresenter = new IconShowPresenter(this);

        boolean ifShowAllIcons = getArguments().getBoolean("ifShowAllIcons", false);
        System.out.println(ifShowAllIcons);
        mPresenter.getAllIcons();
    }

    @Override
    protected boolean isDataComplete() {
        return mIcons != null;
    }

    @Override
    public void onLoadData(List<IconBean> icons) {
        super.onLoadData(icons);

        mIcons = icons;
        RequestManager glide = Glide.with(this);
        rvIcon.setAdapter(new IconAdapter(mIcons, glide));
    }
}