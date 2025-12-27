package com.fam.vest.service;

import com.fam.vest.dto.auth.AuthRequest;
import com.fam.vest.dto.auth.SignupRequest;

public interface SignupService {

    void sendLoginOtp(AuthRequest authRequest);

    void initiateSignup(SignupRequest signupRequest);

    void verifyOtpAndCreateUser(SignupRequest signupRequest);

    boolean validateLoginOtp(AuthRequest authRequest);
}
