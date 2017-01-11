package com.tresorit.zerokitsdk.component;

import com.tresorit.zerokitsdk.activity.SignInActivity;
import com.tresorit.zerokitsdk.module.SignInModule;
import com.tresorit.zerokitsdk.scopes.ActivityScope;

import dagger.Component;

@ActivityScope
@Component(
        modules = {
                SignInModule.class
        },
        dependencies = {
                ApplicationComponent.class
        }
)
public interface SignInComponent {
    void inject(SignInActivity activity);
}
