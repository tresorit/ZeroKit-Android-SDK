package com.tresorit.zerokitsdk.module;

import android.content.Context;

import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.scopes.ApplicationScope;

import dagger.Module;
import dagger.Provides;


@Module
public class ApplicationModule {

    private final ZerokitApplication application;

    public ApplicationModule(ZerokitApplication application) {
        this.application = application;
    }

    @Provides
    @ApplicationScope
    public Context provideContext() {
        return application;
    }
}
