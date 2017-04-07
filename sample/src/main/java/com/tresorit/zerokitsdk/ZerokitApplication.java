package com.tresorit.zerokitsdk;

import android.app.Application;
import android.content.Context;

import com.tresorit.zerokitsdk.component.ApplicationComponent;
import com.tresorit.zerokitsdk.component.DaggerApplicationComponent;
import com.tresorit.zerokitsdk.module.AdminApiModule;
import com.tresorit.zerokitsdk.module.ApplicationModule;

import java.io.IOException;
import java.util.Properties;

public class ZerokitApplication extends Application {

    private ApplicationComponent component;

    public static ZerokitApplication get(Context context) {
        return (ZerokitApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
        Properties properties = new Properties();
        properties.load(getAssets().open("zerokit.properties"));
        component = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .adminApiModule(new AdminApiModule(properties.getProperty("appbackend", ""), properties.getProperty("clientid", "")))
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Invalid config file");
        }
    }

    public ApplicationComponent component() {
        return component;
    }
}
