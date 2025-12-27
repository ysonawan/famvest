package com.fam.vest.exception;

import com.fam.vest.enums.REST_RESPONSE_STATUS;
import com.fam.vest.util.RestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InternalException.class)
    public ResponseEntity<Object> handleInternalException(InternalException ex) {
        log.error("InternalException: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, REST_RESPONSE_STATUS.ERROR, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("ResourceNotFoundException: ", ex);
        return buildResponse(HttpStatus.NOT_FOUND, REST_RESPONSE_STATUS.FAILURE, ex.getMessage());
    }

    @ExceptionHandler(ResourceAlreadyExistException.class)
    public ResponseEntity<Object> handleResourceAlreadyExistException(ResourceAlreadyExistException ex) {
        log.error("ResourceAlreadyExistException: ", ex);
        return buildResponse(HttpStatus.BAD_REQUEST, REST_RESPONSE_STATUS.FAILURE, ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(ValidationException ex) {
        log.error("ValidationException: ", ex);
        return buildResponse(HttpStatus.BAD_REQUEST, REST_RESPONSE_STATUS.FAILURE, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        log.error("handleMethodArgumentNotValidException: {}", errors);
        return buildResponse(HttpStatus.BAD_REQUEST, REST_RESPONSE_STATUS.FAILURE, this.convertWithIteration(errors));
    }

    private String convertWithIteration(Map<String, String> map) {
        StringBuilder mapAsString = new StringBuilder();
        for (String key : map.keySet()) {
            mapAsString.append(key + "=" + map.get(key) + ", ");
        }
        mapAsString.delete(mapAsString.length()-2, mapAsString.length());
        return mapAsString.toString();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        log.error("Generic Exception: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, REST_RESPONSE_STATUS.ERROR, ex.getMessage());
    }

    // ---------------------------------
    // Response Builder Helper
    // ---------------------------------
    private ResponseEntity<Object> buildResponse(HttpStatus status, REST_RESPONSE_STATUS restStatus, String message) {
        RestResponse<Object> response = new RestResponse<>(restStatus, message, String.valueOf(status.value()), message);
        return new ResponseEntity<>(response, status);
    }
}
