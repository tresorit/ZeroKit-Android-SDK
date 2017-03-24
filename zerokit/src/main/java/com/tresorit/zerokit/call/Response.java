package com.tresorit.zerokit.call;

public class Response<T, S> {

    private T result;
    private S error;

    public static <T, S> Response<T, S> fromValue(T value){
        Response<T, S> response = new Response<>();
        response.setResult(value);
        return response;
    }

    public static <T, S> Response<T, S> fromError(S error){
        Response<T, S> response = new Response<>();
        response.setError(error);
        return response;
    }

    public static <T, S> Response<T, S> from(T value, S error){
        Response<T, S> response = new Response<>();
        response.setResult(value);
        response.setError(error);
        return response;
    }

    private Response(){}

    private void setError(S error) {
        this.error = error;
    }

    void setResult(T result) {
        this.result = result;
    }

    public boolean isError() {
        return error != null;
    }

    public T getResult() {
        return result;
    }

    public S getError() {
        return error;
    }
}

