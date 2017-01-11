package com.tresorit.zerokitsdk.module;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.EncryptViewModel;

import dagger.Module;
import dagger.Provides;


@Module
public class EncryptModule {

    private final FragmentManager fragmentManager;

    public EncryptModule(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    @Provides
    @ActivityScope
    public EncryptViewModel provideEncryptViewModel(Context context) {
        return new EncryptViewModel(context, fragmentManager);
    }
}
