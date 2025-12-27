package com.fam.vest.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fam.vest.enums.REST_RESPONSE_STATUS;

@JsonInclude(Include.NON_NULL)
public record RestResponse<T> (REST_RESPONSE_STATUS status, String message, String code, T data) {

}

