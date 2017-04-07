package com.tresorit.zerokit.call;

public abstract class CallAsyncBase<T, S> implements CallAsync<T, S> {
    @Override
    public final void enqueue() {
        enqueue(new Callback<T, S>() {
            @Override
            public void onError(S e) {
            }

            @Override
            public void onSuccess(T result) {
            }
        });
    }

    @Override
    public final void enqueue(final Action<? super T> onSuccess) {
        if (onSuccess == null) {
            throw new IllegalArgumentException("onSuccess can not be null");
        }

        enqueue(new Callback<T, S>() {

            @Override
            public void onError(S e) {
            }

            @Override
            public void onSuccess(T result) {
                onSuccess.call(result);
            }

        });
    }

    @Override
    public final void enqueue(final Action<? super T> onSuccess, final Action<? super S> onFail) {
        if (onSuccess == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onFail == null) {
            throw new IllegalArgumentException("onError can not be null");
        }

        enqueue(new Callback<T, S>() {

            @Override
            public void onError(S e) {
                onFail.call(e);
            }

            @Override
            public void onSuccess(T result) {
                onSuccess.call(result);
            }

        });
    }
}
