package com.tresorit.zerokitsdk.module;

import com.tresorit.adminapi.AdminApi;
import com.tresorit.zerokitsdk.scopes.ApplicationScope;

import dagger.Module;
import dagger.Provides;


@Module
public class AdminApiModule {

    private final String adminUId;
    private final String adminKey;
    private final String apiRoot;

    public AdminApiModule(String adminUId, String adminKey, String apiRoot) {
        this.adminUId = adminUId;
        this.adminKey = adminKey;
        this.apiRoot = apiRoot;
    }

    @Provides
    @ApplicationScope
    public AdminApi provideAdminApi(){
        return new AdminApi(adminUId, adminKey, apiRoot);
    }
}
