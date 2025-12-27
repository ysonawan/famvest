package com.fam.vest.controller;

import com.fam.vest.dto.request.ChangePasswordRequest;
import com.fam.vest.dto.request.UpdateProfileRequest;
import com.fam.vest.dto.response.ApplicationUserDto;
import com.fam.vest.service.IUserProfileService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.UserDetailsUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/rest/v1/users")
@CrossOrigin
@RequiredArgsConstructor
public class ApplicationUserController {

    private final IUserProfileService userProfileService;

    @GetMapping("/profile")
    public ResponseEntity<Object> getUserProfile() {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        ApplicationUserDto profile = userProfileService.getUserProfile(userDetails);
        return CommonUtil.success(profile);
    }

    @PutMapping("/profile/update")
    public ResponseEntity<Object> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        ApplicationUserDto response = userProfileService.updateProfile(userDetails, request);
        return CommonUtil.success(response, "Profile updated successfully");
    }

    @PutMapping("/profile/change-password")
    public ResponseEntity<Object> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        String message = userProfileService.changePassword(userDetails, request);
        return CommonUtil.success(null, message);
    }
}

