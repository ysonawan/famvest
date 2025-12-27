package com.fam.vest.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InternalException(String message) {
        super(message);
    }
}
