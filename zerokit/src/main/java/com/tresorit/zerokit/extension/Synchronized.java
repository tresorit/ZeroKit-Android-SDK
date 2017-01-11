package com.tresorit.zerokit.extension;

import com.tresorit.zerokit.observer.Action1;
import com.tresorit.zerokit.observer.Observable;

import java.util.concurrent.CountDownLatch;

public class Synchronized {

    public static <T, S> Result<T, S> call(Observable<T, S> observable) {
        final Result<T, S> result = new Result<>();
        final CountDownLatch signal = new CountDownLatch(1);
        observable.subscribe(new Action1<T>() {
            @Override
            public void call(T t) {
                result.setResult(t);
                signal.countDown();
            }
        }, new Action1<S>() {
            @Override
            public void call(S s) {
                result.setError(s);
                signal.countDown();
            }
        });
        if (signal.getCount() > 0)
            try {
                signal.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        return result;
    }
}
