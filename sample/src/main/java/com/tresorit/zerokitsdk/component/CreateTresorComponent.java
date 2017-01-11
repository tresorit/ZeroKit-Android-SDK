package com.tresorit.zerokitsdk.component;

import com.tresorit.zerokitsdk.fragment.CreateTresorFragment;
import com.tresorit.zerokitsdk.module.CreateTresorModule;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.CreateTresorViewModel;

import dagger.Component;


@ActivityScope
@Component(
        modules = {
                CreateTresorModule.class
        },
        dependencies = {
                ApplicationComponent.class
        }
)
public interface CreateTresorComponent {
    void inject(CreateTresorFragment fragment);
    CreateTresorViewModel viewmodel();
}
