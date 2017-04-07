package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokitsdk.scopes.ApplicationScope;
import com.tresorit.zerokit.AdminApi;

import dagger.Module;
import dagger.Provides;


@Module
public class AdminApiModule {

    private final String host;
    private final String cliendId;

    public AdminApiModule(String host, String cliendId) {
        this.host = host;
        this.cliendId = cliendId;
    }

    @Provides
    @ApplicationScope
    public AdminApi provideAdminApi(){
        return new AdminApi(host, cliendId);
    }
}
