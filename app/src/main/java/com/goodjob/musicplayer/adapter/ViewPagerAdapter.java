package com.goodjob.musicplayer.adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;


//实现控件滑动效果
public class ViewPagerAdapter extends PagerAdapter {
    private List<String> mTitleList;
    private List<View> mViewList;

    public ViewPagerAdapter(List<String> titleList, List<View> viewList) {
        mTitleList = titleList;
        mViewList = viewList;
    }
//获取控件数量
    @Override
    public int getCount() {
        return mViewList.size();
    }
//开始缓存时，将其初始化
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(mViewList.get(position));
        return mViewList.get(position);
    }
//如果超出缓存范围，即调用destroyItem方法，讲超出范围的销毁
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(mViewList.get(position));
    }
//判断是否为相同的view，只需有返回值即可
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitleList.get(position);
    }
}
