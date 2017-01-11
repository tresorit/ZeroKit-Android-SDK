package com.tresorit.zerokitsdk.component;

import com.tresorit.zerokitsdk.activity.MainActivity;
import com.tresorit.zerokitsdk.module.MainModule;
import com.tresorit.zerokitsdk.scopes.ActivityScope;

import dagger.Component;

@ActivityScope
@Component(
        modules = {
                MainModule.class
        },
        dependencies = {
                ApplicationComponent.class
        }
)
public interface MainComponent {
    void inject(MainActivity activity);
}
