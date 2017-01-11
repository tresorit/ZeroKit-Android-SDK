package com.tresorit.zerokitsdk.viewmodel;

import android.databinding.BaseObservable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;

import com.tresorit.zerokitsdk.message.TabSelectMessage;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

public class MainViewModel extends BaseObservable {

    @SuppressWarnings("WeakerAccess")
    public final BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener;

    @Inject
    public MainViewModel(final EventBus eventBus) {
        onNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                eventBus.post(new TabSelectMessage(item.getItemId()));
                return true;
            }
        };
    }
}
