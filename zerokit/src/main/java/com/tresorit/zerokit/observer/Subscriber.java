package com.tresorit.zerokit.observer;


public abstract class Subscriber<T, S> implements Observer<T, S>, Subscription {

    private boolean unsubscribed = false;

    @Override
    public void unsubscribe() {
        unsubscribed = true;
    }

    @Override
    public boolean isUnsubscribed() {
        return unsubscribed;
    }
}