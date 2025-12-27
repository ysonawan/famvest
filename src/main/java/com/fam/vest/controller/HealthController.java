package com.fam.vest.controller;

import com.fam.vest.enums.REST_RESPONSE_STATUS;
import com.fam.vest.util.RestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/rest/v1/health")
@CrossOrigin
public class HealthController {

    public HealthController() {

    }

    @GetMapping()
    public ResponseEntity<Object> getHealthStatus() {
        String successMessage = "FamVest application up and running";
        log.info("{} as on {}", successMessage, new Date());
        RestResponse<String> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), successMessage);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
