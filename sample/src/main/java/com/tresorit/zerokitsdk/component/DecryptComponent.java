package com.tresorit.zerokitsdk.component;

import com.tresorit.zerokitsdk.fragment.DecryptFragment;
import com.tresorit.zerokitsdk.module.DecryptModule;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.DecryptViewModel;

import dagger.Component;

@ActivityScope
@Component(
        modules = {
                DecryptModule.class
        },
        dependencies = {
                ApplicationComponent.class
        }
)
public interface DecryptComponent {
    void inject(DecryptFragment fragment);
    DecryptViewModel viewmodel();
}
