package com.tresorit.zerokitsdk.cache;

interface ComponentCache {
    long generateId();
    <C> C getComponent(long index);
    <C> void setComponent(long index, C component);
}
