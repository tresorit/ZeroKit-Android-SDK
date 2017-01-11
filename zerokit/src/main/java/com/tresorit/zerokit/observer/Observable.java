package com.tresorit.zerokit.observer;


public class Observable<T, S> {
    private final Observable.OnSubscribe<T, S> onSubscribe;

    public Observable(Observable.OnSubscribe<T, S> f) {
        this.onSubscribe = f;
    }

    public interface OnSubscribe<T, S> extends Action1<Subscriber<? super T, ? super S>> {
    }

    public final Subscription subscribe() {
        return subscribe(new Subscriber<T, S>() {
            @Override
            public void onFail(S e) {
            }

            @Override
            public void onSuccess(T result) {
            }
        });
    }

    public final Subscription subscribe(final Action1<? super T> onSuccess) {
        if (onSuccess == null) {
            throw new IllegalArgumentException("onSuccess can not be null");
        }

        return subscribe(new Subscriber<T, S>() {

            @Override
            public void onFail(S e) {
            }

            @Override
            public void onSuccess(T result) {
                if (!isUnsubscribed()) onSuccess.call(result);
            }

        });
    }

    public final Subscription subscribe(final Action1<? super T> onSuccess, final Action1<? super S> onFail) {
        if (onSuccess == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onFail == null) {
            throw new IllegalArgumentException("onFail can not be null");
        }

        return subscribe(new Subscriber<T, S>() {

            @Override
            public void onFail(S e) {
                if (!isUnsubscribed()) onFail.call(e);
            }

            @Override
            public void onSuccess(T result) {
                if (!isUnsubscribed()) onSuccess.call(result);
            }

        });
    }

    private Subscription subscribe(Subscriber<? super T, ? super S> subscriber) {
        onSubscribe.call(subscriber);
        return subscriber;
    }
}
