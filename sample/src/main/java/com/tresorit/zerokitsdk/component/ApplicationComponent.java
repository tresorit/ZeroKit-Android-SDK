package com.tresorit.zerokitsdk.component;

import android.content.Context;
import android.content.SharedPreferences;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokitsdk.module.AdminApiModule;
import com.tresorit.zerokitsdk.module.ApplicationModule;
import com.tresorit.zerokitsdk.module.EventBusModule;
import com.tresorit.zerokitsdk.module.SharedPreferencesModule;
import com.tresorit.zerokitsdk.module.ZerokitSdkModule;
import com.tresorit.zerokitsdk.scopes.ApplicationScope;
import com.tresorit.zerokit.AdminApi;

import org.greenrobot.eventbus.EventBus;

import dagger.Component;



@ApplicationScope
@Component(
        modules = {
                ApplicationModule.class,
                EventBusModule.class,
                AdminApiModule.class,
                ZerokitSdkModule.class,
                SharedPreferencesModule.class
        }
)
public interface ApplicationComponent {
    Context context();
    EventBus eventbus();
    AdminApi adminApi();
    Zerokit zerokit();
    SharedPreferences sharedpreferences();
}
