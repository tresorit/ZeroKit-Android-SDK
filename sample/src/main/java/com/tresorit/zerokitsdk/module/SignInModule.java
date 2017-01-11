package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.SignInViewModel;

import org.greenrobot.eventbus.EventBus;

import dagger.Module;
import dagger.Provides;

@Module
public class SignInModule {
    @Provides
    @ActivityScope
    public SignInViewModel provideSignInViewModel(EventBus eventBus) {
        return new SignInViewModel(eventBus);
    }
}
