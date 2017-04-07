package com.tresorit.zerokit.call;

public interface Call<T, S> extends CallAsync<T, S> {
    Response<T, S> execute();
}
