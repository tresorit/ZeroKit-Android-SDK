package com.tresorit.zerokit;

interface PasswordHandler {
    int length();
    boolean isEmpty();
    void clear();
    boolean isContentEqual(PasswordEditText passwordEditText);
    boolean isContentEqual(PasswordEditText.PasswordExporter exporter);
}
