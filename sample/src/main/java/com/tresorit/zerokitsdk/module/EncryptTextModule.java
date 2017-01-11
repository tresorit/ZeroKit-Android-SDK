package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.EncryptTextViewModel;

import org.greenrobot.eventbus.EventBus;

import dagger.Module;
import dagger.Provides;


@Module
public class EncryptTextModule {
    @Provides
    @ActivityScope
    public EncryptTextViewModel provideEncryptTextViewModel(Zerokit zerokit, EventBus eventBus) {
        return new EncryptTextViewModel(zerokit, eventBus);
    }
}
