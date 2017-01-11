package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.DecryptViewModel;

import dagger.Module;
import dagger.Provides;

@Module
public class DecryptModule {
    @Provides
    @ActivityScope
    public DecryptViewModel provideEncryptTextViewModel(Zerokit zerokit) {
        return new DecryptViewModel(zerokit);
    }
}
