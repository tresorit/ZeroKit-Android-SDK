package com.tresorit.zerokit.observer;

public interface Subscription {
    void unsubscribe();

    boolean isUnsubscribed();
}
