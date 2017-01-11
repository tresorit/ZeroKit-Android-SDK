package com.tresorit.zerokitsdk.module;

import com.tresorit.zerokitsdk.scopes.ApplicationScope;

import org.greenrobot.eventbus.EventBus;

import dagger.Module;
import dagger.Provides;

@Module
public class EventBusModule {
    @Provides
    @ApplicationScope
    public EventBus provideEventBus() {
        return EventBus.getDefault();
    }
}
