package com.tresorit.zerokitsdk.module;

import android.content.SharedPreferences;

import com.tresorit.adminapi.AdminApi;
import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.ShareTresorViewModel;

import dagger.Module;
import dagger.Provides;

@Module
public class ShareTresorModule {
    @Provides
    @ActivityScope
    public ShareTresorViewModel provideShareTresorViewModel(Zerokit zerokit, AdminApi adminApi, SharedPreferences sharedPreferences) {
        return new ShareTresorViewModel(zerokit, adminApi, sharedPreferences);
    }
}
