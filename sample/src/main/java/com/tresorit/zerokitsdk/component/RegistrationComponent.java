package com.tresorit.zerokitsdk.component;

import com.tresorit.zerokitsdk.fragment.SignUpFragment;
import com.tresorit.zerokitsdk.module.RegistrationModule;
import com.tresorit.zerokitsdk.scopes.ActivityScope;

import dagger.Component;

@ActivityScope
@Component(
        modules = {
                RegistrationModule.class
        },
        dependencies = {
                ApplicationComponent.class
        }
)
public interface RegistrationComponent {
    void inject(SignUpFragment fragment);
}
