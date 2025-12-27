package com.fam.vest.controller;

import com.fam.vest.enums.REST_RESPONSE_STATUS;
import com.fam.vest.dto.response.PositionDetails;
import com.fam.vest.service.PositionService;
import com.fam.vest.util.RestResponse;
import com.fam.vest.util.UserDetailsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/rest/v1/positions")
@CrossOrigin
public class PositionController {

    private final PositionService positionService;

    @Autowired
    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping()
    public ResponseEntity<Object> getPositions(@RequestParam(value = "type", required = false) Optional<String> type,
                                               @RequestParam(value = "userId", required = false) Optional<String> tradingAccountId) {

        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching positions for type: {}, tradingAccountId: {} by: {}", type.orElse("net"), tradingAccountId.orElse("all"), userDetails.getUsername());
        List<PositionDetails> positions = positionService.getPositions(userDetails, type, tradingAccountId);
        RestResponse<List<PositionDetails>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), positions);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
