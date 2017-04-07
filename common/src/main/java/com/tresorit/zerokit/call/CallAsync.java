package com.tresorit.zerokit.call;

public interface CallAsync<T, S> {
    void enqueue();
    void enqueue(Action<? super T> onSuccess);
    void enqueue(Action<? super T> onSuccess, Action<? super S> onFail);
    void enqueue(Callback<? super T, ? super S> callback);
}
