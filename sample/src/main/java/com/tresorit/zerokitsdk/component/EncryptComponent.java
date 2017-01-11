package com.tresorit.zerokitsdk.component;

import com.tresorit.zerokitsdk.fragment.EncryptFragment;
import com.tresorit.zerokitsdk.module.EncryptModule;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.EncryptViewModel;

import dagger.Component;

@ActivityScope
@Component(
        modules = {
                EncryptModule.class
        },
        dependencies = {
                ApplicationComponent.class
        }
)
public interface EncryptComponent {
    void inject(EncryptFragment fragment);
    EncryptViewModel viewmodel();
}
