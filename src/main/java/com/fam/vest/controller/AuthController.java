package com.fam.vest.controller;

import com.fam.vest.auth.JwtUtil;
import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.enums.REST_RESPONSE_STATUS;
import com.fam.vest.exception.UserNotFoundException;
import com.fam.vest.dto.auth.AuthRequest;
import com.fam.vest.dto.auth.AuthResponse;
import com.fam.vest.dto.auth.SignupRequest;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.service.SignupService;
import com.fam.vest.service.TradingAccountService;
import com.fam.vest.util.RestResponse;
import com.zerodhatech.models.Profile;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/rest/auth")
public class AuthController {

    private final ApplicationUserRepository applicationUserRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final TradingAccountService tradingAccountService;
    private final SignupService signupService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        AtomicBoolean isAdmin = new AtomicBoolean(false);
        if(StringUtils.isNotBlank(authRequest.getOtp())) {
            ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(authRequest.getEmail().toLowerCase());
            if(null == applicationUser) {
                throw new UserNotFoundException("User not found with email: " + authRequest.getEmail());
            }
            if(applicationUser.getRole().equals("ADMIN")) {
                isAdmin.set(true);
            }
            signupService.validateLoginOtp(authRequest);
        } else {
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );
            authenticate.getAuthorities().forEach(grantedAuthority -> {
                isAdmin.set(grantedAuthority.getAuthority().equals("ADMIN"));
            });
        }
        String token = jwtUtil.generateToken(authRequest.getEmail(), isAdmin);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> verifyOtpAndCreateUser(@Valid @RequestBody SignupRequest signupRequest) {
        signupService.verifyOtpAndCreateUser(signupRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signup/send-otp")
    public ResponseEntity<?> sendSignupOtp(@Valid @RequestBody SignupRequest signupRequest) {
        signupService.initiateSignup(signupRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login/send-otp")
    public ResponseEntity<?> sendLoginOtp(@Valid @RequestBody AuthRequest authRequest) {
        signupService.sendLoginOtp(authRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/register-request-token")
    public ResponseEntity<Object> registerRequestToken(@RequestParam("user_id") String userId,
                                                       @RequestParam("name") String name,
                                                       @RequestParam("request_token") String requestToken,
                                                       @RequestParam("status") String status) {
        log.info("Registering request token for userId: {}, name: {}, requestToken: {}, status: {}", userId, name, requestToken, status);
        Profile profile = tradingAccountService.registerRequestToken(userId, requestToken, status);
        log.info("Registered request token for user short name: {}, user name: {}", profile.userShortname, profile.userName);
        RestResponse<List<TradingAccount>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, "Request token registered successfully.",
                String.valueOf(HttpStatus.OK.value()), null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
