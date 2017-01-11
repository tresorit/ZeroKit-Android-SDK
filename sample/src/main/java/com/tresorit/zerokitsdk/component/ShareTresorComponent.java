package com.tresorit.zerokitsdk.component;

import com.tresorit.zerokitsdk.fragment.ShareTresorFragment;
import com.tresorit.zerokitsdk.module.ShareTresorModule;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.ShareTresorViewModel;

import dagger.Component;

@ActivityScope
@Component(
        modules = {
                ShareTresorModule.class
        },
        dependencies = {
                ApplicationComponent.class
        }
)
public interface ShareTresorComponent {
    void inject(ShareTresorFragment fragment);
    ShareTresorViewModel viewmodel();
}
