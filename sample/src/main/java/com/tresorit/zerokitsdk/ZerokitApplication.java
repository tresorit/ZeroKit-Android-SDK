package com.tresorit.zerokitsdk;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

import com.tresorit.zerokitsdk.component.ApplicationComponent;
import com.tresorit.zerokitsdk.component.DaggerApplicationComponent;
import com.tresorit.zerokitsdk.module.AdminApiModule;
import com.tresorit.zerokitsdk.module.ApplicationModule;

import java.io.IOException;
import java.util.Properties;

import static com.tresorit.zerokit.Zerokit.API_ROOT;

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
                    .adminApiModule(new AdminApiModule(properties.getProperty("adminuserid", ""), properties.getProperty("adminkey", ""), getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData.getString(API_ROOT)))
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Invalid config file");
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("No ApiRoot definition found in the AndroidManifest.xml");
        }
    }

    public ApplicationComponent component() {
        return component;
    }
}
