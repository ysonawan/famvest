package com.fam.vest.service;

import com.fam.vest.dto.request.ChangePasswordRequest;
import com.fam.vest.dto.request.UpdateProfileRequest;
import com.fam.vest.dto.response.ApplicationUserDto;
import org.springframework.security.core.userdetails.UserDetails;

public interface IUserProfileService {

    ApplicationUserDto getUserProfile(UserDetails userDetails);
    ApplicationUserDto updateProfile(UserDetails userDetails, UpdateProfileRequest request);
    String changePassword(UserDetails userDetails, ChangePasswordRequest request);
}

