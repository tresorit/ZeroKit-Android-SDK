package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.MainViewModel;

import org.greenrobot.eventbus.EventBus;

import dagger.Module;
import dagger.Provides;

@Module
public class MainModule {
    @Provides
    @ActivityScope
    public MainViewModel provideMainViewModel(EventBus eventBus) {
        return new MainViewModel(eventBus);
    }
}
