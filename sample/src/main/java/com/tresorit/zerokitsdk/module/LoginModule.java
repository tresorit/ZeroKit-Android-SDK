package com.tresorit.zerokitsdk.module;

import android.content.SharedPreferences;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.LoginViewModel;

import org.greenrobot.eventbus.EventBus;

import dagger.Module;
import dagger.Provides;

@Module
public class LoginModule {
    @Provides
    @ActivityScope
    public LoginViewModel provideLoginViewModel(Zerokit zerokit, EventBus eventBus, SharedPreferences sharedPreferences) {
        return new LoginViewModel(zerokit,  eventBus, sharedPreferences);
    }
}
