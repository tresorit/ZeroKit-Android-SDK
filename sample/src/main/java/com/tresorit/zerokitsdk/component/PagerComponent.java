package com.tresorit.zerokitsdk.component;

import com.tresorit.zerokitsdk.adapter.EncryptPagerAdapter;
import com.tresorit.zerokitsdk.fragment.CreateTresorFragment;
import com.tresorit.zerokitsdk.fragment.EncryptTextFragment;
import com.tresorit.zerokitsdk.fragment.ShareTresorFragment;
import com.tresorit.zerokitsdk.module.PagerModule;
import com.tresorit.zerokitsdk.scopes.ActivityScope;

import dagger.Component;

@ActivityScope
@Component(
        modules = {
                PagerModule.class,
        },
        dependencies = {
                ApplicationComponent.class
        }
)
public interface PagerComponent extends ApplicationComponent {
    void inject(EncryptPagerAdapter fragment);

    CreateTresorFragment createTresorFragment();
    EncryptTextFragment encryptTextFragment();
    ShareTresorFragment shareTresorFragment();
}
