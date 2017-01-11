package com.tresorit.zerokitsdk.message;

public class ShowMessageMessage {

    private final String message;

    public ShowMessageMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}