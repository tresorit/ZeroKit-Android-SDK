package com.tresorit.zerokitsdk.cache;

import android.support.annotation.NonNull;

interface ComponentFactory<C> {
    /**
     * Create a new instance of a Component
     *
     * @return The Component instance
     */
    @NonNull C createComponent();
}
