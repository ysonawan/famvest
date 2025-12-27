package com.fam.vest.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class RESTResponseErrorHandler implements ResponseErrorHandler {

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return new DefaultResponseErrorHandler().hasError(response);
	}

	@Override
	public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
		HttpStatus statusCode = HttpStatus.valueOf(response.getStatusCode().value());
		String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
		log.error("Method: {} URL: {} Status Code: {} Response: {}", method, url, statusCode, responseBody);
	}

}
