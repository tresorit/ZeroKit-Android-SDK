package com.tresorit.zerokitsdk.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;

import com.tresorit.zerokitsdk.R;
import com.tresorit.zerokitsdk.adapter.EncryptPagerAdapter;

public class EncryptViewModel extends BaseObservable {

    @SuppressWarnings("WeakerAccess")
    public final EncryptPagerAdapter pagerAdapter;
    @SuppressWarnings("WeakerAccess")
    public final ViewPager.OnPageChangeListener pageChangeListener;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<Drawable> drawableDot1;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<Drawable> drawableDot2;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<Drawable> drawableDot3;


    public EncryptViewModel(final Context context, FragmentManager fragmentManager) {
        pagerAdapter = new EncryptPagerAdapter(context, fragmentManager);
        pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                drawableDot1.set(context.getResources().getDrawable(position == 0 ? R.drawable.selecteditem_dot : R.drawable.nonselecteditem_dot));
                drawableDot2.set(context.getResources().getDrawable(position == 1 ? R.drawable.selecteditem_dot : R.drawable.nonselecteditem_dot));
                drawableDot3.set(context.getResources().getDrawable(position == 2 ? R.drawable.selecteditem_dot : R.drawable.nonselecteditem_dot));
            }

        };

        drawableDot1 = new ObservableField<>(context.getResources().getDrawable(R.drawable.selecteditem_dot));
        drawableDot2 = new ObservableField<>(context.getResources().getDrawable(R.drawable.nonselecteditem_dot));
        drawableDot3 = new ObservableField<>(context.getResources().getDrawable(R.drawable.nonselecteditem_dot));
    }



}
