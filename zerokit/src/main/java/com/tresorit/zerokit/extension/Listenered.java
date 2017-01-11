package com.tresorit.zerokit.extension;

import com.tresorit.zerokit.observer.Action1;
import com.tresorit.zerokit.observer.Observable;

public class Listenered {

    public static <T, S> void call(Observable<T, S> observable, final Listener<T, S> listener) {
        observable.subscribe(new Action1<T>() {
            @Override
            public void call(T t) {
                listener.onFinished(Result.<T, S>fromValue(t));
            }
        }, new Action1<S>() {
            @Override
            public void call(S s) {
                listener.onFinished(Result.<T, S>fromError(s));
            }
        });
    }
}
