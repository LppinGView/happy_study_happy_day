package com.redis.demo.exceptions;

public class ArgumentCheckFailException extends ApiException {
    public ArgumentCheckFailException(String code, String message) {
        super(code, message);
    }
}
