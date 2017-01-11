package com.tresorit.zerokitsdk.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.component.DaggerPagerComponent;
import com.tresorit.zerokitsdk.fragment.CreateTresorFragment;
import com.tresorit.zerokitsdk.fragment.EncryptTextFragment;
import com.tresorit.zerokitsdk.fragment.ShareTresorFragment;

import javax.inject.Inject;

public class EncryptPagerAdapter extends FragmentPagerAdapter {

    @SuppressWarnings("WeakerAccess")
    @Inject
    CreateTresorFragment createTresorFragment;

    @SuppressWarnings("WeakerAccess")
    @Inject
    EncryptTextFragment encryptTextFragment;

    @SuppressWarnings("WeakerAccess")
    @Inject
    ShareTresorFragment shareTresorFragment;


    public EncryptPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        DaggerPagerComponent.builder().applicationComponent(ZerokitApplication.get(context).component()).build().inject(this);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return createTresorFragment;
            case 1:
                return encryptTextFragment;
            case 2:
                return shareTresorFragment;
            default:
                return new Fragment();
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}
