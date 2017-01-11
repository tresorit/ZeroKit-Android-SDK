package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokitsdk.scopes.ApplicationScope;

import dagger.Module;
import dagger.Provides;

@Module
public class ZerokitSdkModule {

    @Provides
    @ApplicationScope
    public Zerokit provideZerokit(){
        return Zerokit.getInstance();
    }
}
