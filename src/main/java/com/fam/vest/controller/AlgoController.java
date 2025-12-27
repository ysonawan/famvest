package com.fam.vest.controller;

import com.fam.vest.dto.request.StatusUpdateRequest;
import com.fam.vest.dto.request.StraddleStrategyRequest;
import com.fam.vest.entity.StraddleStrategy;
import com.fam.vest.entity.StraddleStrategyExecution;
import com.fam.vest.service.AlgoService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.UserDetailsUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/rest/v1/algo")
@CrossOrigin
public class AlgoController {

    private final AlgoService algoService;

    public AlgoController(AlgoService algoService) {
        this.algoService = algoService;
    }

    @GetMapping("/straddles")
    public ResponseEntity<Object> getStraddleStrategies(@RequestParam(value = "active", required = false) Optional<Boolean> active) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching straddle strategies by: {}", userDetails.getUsername());
        List<StraddleStrategy> straddleStrategies = algoService.getStraddleStrategies(userDetails, active);
        return CommonUtil.success(straddleStrategies);
    }

    @GetMapping("/straddles/executions")
    public ResponseEntity<Object> getStraddleStrategyExecutions() {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching straddle strategy executions by: {}", userDetails.getUsername());
        List<StraddleStrategyExecution> straddleStrategyExecutions = algoService.getStraddleStrategyExecutions(userDetails);
        return CommonUtil.success(straddleStrategyExecutions);
    }

    @GetMapping("/straddles/executions/{straddleId}")
    public ResponseEntity<Object> getStraddleStrategyExecutionsByStraddleId(@PathVariable ("straddleId") Long straddleId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching straddle strategy executions by: {} for: straddle id: {}", userDetails.getUsername(), straddleId);
        List<StraddleStrategyExecution> straddleStrategyExecutions = algoService.getStraddleStrategyExecutionsByStraddleId(userDetails, straddleId);
        return CommonUtil.success(straddleStrategyExecutions);
    }

    @PostMapping("/straddles")
    public ResponseEntity<Object> saveStraddleStrategies(@Valid @RequestBody StraddleStrategyRequest straddleStrategyRequest) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Saving straddle strategy {} by: {}", straddleStrategyRequest, userDetails.getUsername());
        StraddleStrategy straddleStrategy = algoService.saveStraddleStrategy(userDetails, straddleStrategyRequest);
        return CommonUtil.success(straddleStrategy);
    }

    @PutMapping("/straddles/{id}")
    public ResponseEntity<Object> updateStraddleStrategies(@PathVariable ("id") Long id, @Valid @RequestBody StraddleStrategyRequest straddleStrategyRequest) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Updating straddle strategy {} by: {}", straddleStrategyRequest, userDetails.getUsername());
        StraddleStrategy straddleStrategy = algoService.updateStraddleStrategy(userDetails, id, straddleStrategyRequest);
        return CommonUtil.success(straddleStrategy);
    }

    @DeleteMapping("/straddles/{id}")
    public ResponseEntity<Object> deleteStraddleStrategy(@PathVariable ("id") Long id) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Deleting straddle strategy id {} by: {}", id, userDetails.getUsername());
        algoService.deleteStraddleStrategy(id, userDetails);
        return CommonUtil.success(null, "Straddle strategy status deleted successfully.");
    }

    @GetMapping("/straddles/{id}")
    public ResponseEntity<Object> getStraddleStrategy(@PathVariable ("id") Long id) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching straddle strategy for id: {} by: {}", id, userDetails.getUsername());
        StraddleStrategy straddleStrategy = algoService.getStraddleStrategy(userDetails, id);
        return CommonUtil.success(straddleStrategy);
    }

    @PatchMapping("/straddles/{id}/status")
    public ResponseEntity<Object> updateStraddleStrategyStatus(@PathVariable ("id") Long id,
                                             @Valid @RequestBody StatusUpdateRequest request) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Updating straddle strategy status for id: {} by: {}", id, userDetails.getUsername());
        StraddleStrategy straddleStrategy = algoService.updateStraddleStrategyStatus(userDetails, id, request);
        return CommonUtil.success(straddleStrategy, "Straddle strategy status updated successfully.");
    }

    @PostMapping("/straddles/{id}/execute")
    public ResponseEntity<Object> executeStraddleStrategy(@PathVariable ("id") Long id) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Executing straddle strategy for id: {} by: {}", id, userDetails.getUsername());
        StraddleStrategy straddleStrategy = algoService.executeStraddleStrategy(userDetails, id);
        return CommonUtil.success(straddleStrategy, "Straddle executed successfully. Please check your orders.");
    }
}
