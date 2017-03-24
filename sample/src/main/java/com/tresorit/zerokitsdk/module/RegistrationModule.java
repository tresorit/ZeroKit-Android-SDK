package com.tresorit.zerokitsdk.module;

import android.content.Context;
import android.content.SharedPreferences;

import com.tresorit.adminapi.AdminApi;
import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.RegistrationViewModel;

import org.greenrobot.eventbus.EventBus;

import dagger.Module;
import dagger.Provides;

@Module
public class RegistrationModule {
    @Provides
    @ActivityScope
    public RegistrationViewModel provideRegistrationViewModel(Zerokit zerokit, AdminApi adminApi, EventBus eventbus, SharedPreferences sharedPreferences, Context context) {
        return new RegistrationViewModel(zerokit, adminApi, eventbus, sharedPreferences, context.getResources());
    }
}
