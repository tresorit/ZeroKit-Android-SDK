package com.tresorit.zerokit.observer;

/**
 * A one-argument action.
 *  @param <T> the first argument type
 */
public interface Action1<T> extends Action {
    void call(T t);
}
