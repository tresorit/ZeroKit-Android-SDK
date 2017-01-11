package com.tresorit.zerokitsdk.cache;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public abstract class ComponentControllerFragment<C> extends Fragment {

    private final ComponentControllerDelegate<C> componentDelegate = new ComponentControllerDelegate<>();

    @Override
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentCache componentCache = getComponentCache();
        componentDelegate.onCreate(componentCache, savedInstanceState, componentFactory);
    }

    @Override
    @CallSuper
    public void onResume() {
        super.onResume();
        componentDelegate.onResume();
    }

    @Override
    @CallSuper
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        componentDelegate.onSaveInstanceState(outState);
    }

    @Override
    @CallSuper
    public void onDestroy() {
        super.onDestroy();
        componentDelegate.onDestroy();
    }

    private ComponentCache getComponentCache() {
        return (ComponentCache) getActivity();
    }

    protected C getComponent() {
        return componentDelegate.getComponent();
    }

    protected abstract C onCreateNonConfigurationComponent();

    private final ComponentFactory<C> componentFactory = new ComponentFactory<C>() {
        @NonNull
        @Override
        public C createComponent() {
            return onCreateNonConfigurationComponent();
        }
    };
}