package com.redis.demo.exceptions;

import feign.FeignException;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApiException
    extends FeignException
    implements UserExceptionMessage {
    private String code;

    public ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
