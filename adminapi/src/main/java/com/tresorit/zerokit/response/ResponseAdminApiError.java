package com.tresorit.zerokit.response;

public class ResponseAdminApiError {
    private String code;
    private String message;
    private ResponseExtensionException exception;

    public ResponseAdminApiError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("code: %s, message: %s", code, message);
    }
}
