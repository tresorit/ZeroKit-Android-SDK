package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokitsdk.fragment.CreateTresorFragment;
import com.tresorit.zerokitsdk.fragment.EncryptTextFragment;
import com.tresorit.zerokitsdk.fragment.ShareTresorFragment;
import com.tresorit.zerokitsdk.scopes.ActivityScope;

import dagger.Module;
import dagger.Provides;

@Module
public class PagerModule {

    @Provides
    @ActivityScope
    public CreateTresorFragment provideCreateTresorFragment() {
        return new CreateTresorFragment();
    }

    @Provides
    @ActivityScope
    public ShareTresorFragment provideShareTresorFragment() {
        return new ShareTresorFragment();
    }

    @Provides
    @ActivityScope
    public EncryptTextFragment provideEncryptTextFragment() {
        return new EncryptTextFragment();
    }
}
