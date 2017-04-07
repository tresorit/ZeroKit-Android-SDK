package com.tresorit.zerokit.call;

public interface Callback<T, S> {

    void onSuccess(T result);

    void onError(S e);

}

