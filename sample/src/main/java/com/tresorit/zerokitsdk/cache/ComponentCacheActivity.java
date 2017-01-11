package com.tresorit.zerokitsdk.cache;

import android.os.Bundle;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AppCompatActivity;

import java.util.concurrent.atomic.AtomicLong;

public abstract class ComponentCacheActivity extends AppCompatActivity implements ComponentCache {
    private static final String NEXT_ID_KEY = "next-presenter-id";

    private NonConfigurationInstance nonConfigurationInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        nonConfigurationInstance = (NonConfigurationInstance) getLastCustomNonConfigurationInstance();
        if (nonConfigurationInstance == null) {
            long seed;

            if (savedInstanceState == null) seed = 0;
            else seed = savedInstanceState.getLong(NEXT_ID_KEY);

            nonConfigurationInstance = new NonConfigurationInstance(seed);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(NEXT_ID_KEY, nonConfigurationInstance.nextId.get());
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return nonConfigurationInstance;
    }

    @Override
    public long generateId() {
        return nonConfigurationInstance.nextId.getAndIncrement();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <C> C getComponent(long index) {
        return (C) nonConfigurationInstance.components.get(index);
    }

    @Override
    public void setComponent(long index, Object component) {
        nonConfigurationInstance.components.put(index, component);
    }


    private static class NonConfigurationInstance {
        final LongSparseArray<Object> components;
        final AtomicLong nextId;

        NonConfigurationInstance(long seed) {
            components = new LongSparseArray<>();
            nextId = new AtomicLong(seed);
        }
    }
}
