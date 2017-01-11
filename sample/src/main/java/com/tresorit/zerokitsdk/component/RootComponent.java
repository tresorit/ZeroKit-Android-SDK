package com.tresorit.zerokitsdk.component;

import com.tresorit.zerokitsdk.activity.RootActivity;
import com.tresorit.zerokitsdk.scopes.ActivityScope;

import dagger.Component;

@ActivityScope
@Component(
        dependencies = {
                ApplicationComponent.class
        }
)
public interface RootComponent {
    void inject(RootActivity activity);
}
