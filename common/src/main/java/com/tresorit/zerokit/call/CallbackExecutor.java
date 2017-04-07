package com.tresorit.zerokit.call;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CallbackExecutor<T, S> implements Callback<T, S> {

    @SuppressWarnings("WeakerAccess")
    static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    @SuppressWarnings("WeakerAccess")
    final Callback<? super T, ? super S> callback;

    public CallbackExecutor(Callback<? super T, ? super S> callback) {
        this.callback = callback;
    }

    @Override
    public void onSuccess(final T result) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(result);
            }
        });
    }

    @Override
    public void onError(final S error) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }
}
