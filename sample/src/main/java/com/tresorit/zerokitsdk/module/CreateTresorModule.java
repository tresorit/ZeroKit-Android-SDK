package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.CreateTresorViewModel;
import com.tresorit.zerokit.AdminApi;

import org.greenrobot.eventbus.EventBus;

import dagger.Module;
import dagger.Provides;

@Module
public class CreateTresorModule {
    @Provides
    @ActivityScope
    public CreateTresorViewModel provideCreateTresorViewModel(Zerokit zerokit, AdminApi adminApi, EventBus eventBus) {
        return new CreateTresorViewModel(zerokit, adminApi, eventBus);
    }
}
