package com.tresorit.zerokitsdk.component;

import com.tresorit.zerokitsdk.fragment.EncryptTextFragment;
import com.tresorit.zerokitsdk.module.EncryptTextModule;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.EncryptTextViewModel;

import dagger.Component;

@ActivityScope
@Component(
        modules = {
                EncryptTextModule.class
        },
        dependencies = {
                ApplicationComponent.class
        }
)
public interface EncryptTextComponent {
    void inject(EncryptTextFragment fragment);
    EncryptTextViewModel viewmodel();
}
