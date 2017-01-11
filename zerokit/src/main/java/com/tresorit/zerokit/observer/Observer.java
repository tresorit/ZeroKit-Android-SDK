package com.tresorit.zerokit.observer;

/**
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * After an Observer calls an {@link Observable}'s {@link Observable#subscribe subscribe} method. A well-behaved
 * {@code Observable} will call an Observer's {@link #onSuccess} method exactly once or the Observer's
 * {@link #onFail} method exactly once.
 *
 * @param <T>
 *          the type of item the Observer expects to observe
 * @param <S>
 *          the type of item the Observer expects as error
 */
public interface Observer<T, S> {

    /**
     * Notifies the Observer that the {@link Observable} has finished sending push-based notifications.
     * The {@link Observable} will not call this method if it calls {@link #onFail}.
     *
     * @param result the item emitted by the Observable
     */
    void onSuccess(T result);

    /**
     * Notifies the Observer that the {@link Observable} has experienced an error condition.
     * <p>
     * If the {@link Observable} calls this method, it will not thereafter call
     * {@link #onSuccess}.
     *
     * @param e the error encountered by the Observable
     */
    void onFail(S e);

}

