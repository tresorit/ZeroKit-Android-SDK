package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.ShareTresorViewModel;
import com.tresorit.zerokit.AdminApi;

import org.greenrobot.eventbus.EventBus;

import dagger.Module;
import dagger.Provides;

@Module
public class ShareTresorModule {
    @Provides
    @ActivityScope
    public ShareTresorViewModel provideShareTresorViewModel(Zerokit zerokit, AdminApi adminApi, EventBus eventBus) {
        return new ShareTresorViewModel(zerokit, adminApi, eventBus);
    }
}
