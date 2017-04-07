package com.tresorit.zerokit.call;

public class CallAsyncAction<T, S> extends CallAsyncBase<T, S> {

    protected final ActionCallback<T, S> action;

    public CallAsyncAction(ActionCallback<T, S> action) {
        this.action = action;
    }

    @Override
    public void enqueue(Callback<? super T, ? super S> callback) {
        action.call(callback);
    }
}
