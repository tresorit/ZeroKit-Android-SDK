package com.tresorit.zerokitsdk.viewmodel;

import android.databinding.BaseObservable;
import android.databinding.ObservableInt;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;

import com.tresorit.zerokitsdk.R;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

public class SignInViewModel extends BaseObservable {

    @SuppressWarnings("WeakerAccess")
    public final BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener;
    @SuppressWarnings("WeakerAccess")
    public final ObservableInt displayedChild;

    @Inject
    public SignInViewModel(@SuppressWarnings("UnusedParameters") EventBus eventBus) {
        displayedChild = new ObservableInt(0);
        onNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                displayedChild.set(item.getItemId() == R.id.tab_signin ? 0 : 1);
                return true;
            }
        };
    }
}
