package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokitsdk.scopes.ApplicationScope;
import com.tresorit.zerokit.AdminApi;

import dagger.Module;
import dagger.Provides;


@Module
public class AdminApiModule {

    public AdminApiModule(String host, String cliendId) {
        AdminApi.init(host, cliendId);
    }

    @Provides
    @ApplicationScope
    public AdminApi provideAdminApi(){
        return AdminApi.getInstance();
    }
}
