package com.tresorit.zerokit.call;

/**
 * A one-argument action.
 *  @param <T> the first argument type
 */
public interface Action<T> {
    void call(T t);
}
