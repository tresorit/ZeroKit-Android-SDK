package com.tresorit.zerokit.call;

import android.os.Handler;
import android.os.Looper;

import com.tresorit.zerokit.util.Holder;

import java.util.concurrent.CountDownLatch;

public class CallAction<T, S> extends CallAsyncAction<T, S> implements Call<T, S> {

    public CallAction(ActionCallback<T, S> action) {
        super(action);
    }

    private void call(final Callback<? super T, ? super S> callback, boolean sync) {
        if (sync) action.call(callback);
        else {
            if (Looper.myLooper() == null)
                throw new IllegalStateException("Asynchronous method only possible from looper threads.");
            final Handler handler = new Handler(Looper.myLooper());
            action.call(new Callback<T, S>() {
                @Override
                public void onSuccess(final T result) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(result);
                        }
                    });
                }

                @Override
                public void onError(final S e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e);
                        }
                    });
                }
            });
        }
    }

    @Override
    public final void enqueue(final Callback<? super T, ? super S> callback) {
        call(callback, false);
    }

    @Override
    public final Response<T, S> execute() {
        final Holder<Response<T, S>> result = new Holder<>();
        final CountDownLatch signal = new CountDownLatch(1);
        call(new Callback<T, S>() {
            @Override
            public void onSuccess(T t) {
                result.t = Response.fromValue(t);
                signal.countDown();
            }

            @Override
            public void onError(S e) {
                result.t = Response.fromError(e);
                signal.countDown();
            }
        }, true);
        if (signal.getCount() > 0)
            try {
                signal.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        return result.t;
    }
}
