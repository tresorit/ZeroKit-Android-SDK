package com.tresorit.zerokit.extension;

public class Result<T, S> {

    private T result;
    private S error;

    public static <T, S> Result<T, S> fromValue(T value){
        Result<T, S> result = new Result<>();
        result.setResult(value);
        return result;
    }

    public static <T, S> Result<T, S> fromError(S error){
        Result<T, S> result = new Result<>();
        result.setError(error);
        return result;
    }

    public static <T, S> Result<T, S> from(T value, S error){
        Result<T, S> result = new Result<>();
        result.setResult(value);
        result.setError(error);
        return result;
    }

    Result(){}

    void setError(S error) {
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

