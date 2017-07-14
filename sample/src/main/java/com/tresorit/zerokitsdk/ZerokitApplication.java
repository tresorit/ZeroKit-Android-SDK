package com.tresorit.zerokitsdk;

import android.app.Application;
import android.content.Context;

import com.tresorit.zerokitsdk.component.ApplicationComponent;
import com.tresorit.zerokitsdk.component.DaggerApplicationComponent;
import com.tresorit.zerokitsdk.module.AdminApiModule;
import com.tresorit.zerokitsdk.module.ApplicationModule;

public class ZerokitApplication extends Application {

    private ApplicationComponent component;

    public static ZerokitApplication get(Context context) {
        return (ZerokitApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        component = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .adminApiModule(new AdminApiModule(BuildConfig.APP_BACKEND, BuildConfig.CLIENT_ID))
                .build();
    }

    public ApplicationComponent component() {
        return component;
    }
}
