package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.LoginViewModel;
import com.tresorit.zerokit.AdminApi;

import org.greenrobot.eventbus.EventBus;

import dagger.Module;
import dagger.Provides;

@Module
public class LoginModule {
    @Provides
    @ActivityScope
    public LoginViewModel provideLoginViewModel(Zerokit zerokit, AdminApi adminApi, EventBus eventBus) {
        return new LoginViewModel(zerokit,  adminApi, eventBus);
    }
}
