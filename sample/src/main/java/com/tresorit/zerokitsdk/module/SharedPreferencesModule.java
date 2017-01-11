package com.tresorit.zerokitsdk.module;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.tresorit.zerokitsdk.scopes.ApplicationScope;

import dagger.Module;
import dagger.Provides;

@Module
public class SharedPreferencesModule {
    @Provides
    @ApplicationScope
    public SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
