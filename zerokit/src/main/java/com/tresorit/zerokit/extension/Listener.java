package com.tresorit.zerokit.extension;

public interface Listener<T, S> {

    void onFinished(Result<T, S> result);

}

