package com.tresorit.zerokitsdk.message;


public class CopyEncryptedTextMessage {
    private final String encryptedMessage;

    public CopyEncryptedTextMessage(String encryptedMessage) {
        this.encryptedMessage = encryptedMessage;
    }

    public String getEncryptedMessage() {
        return encryptedMessage;
    }
}
