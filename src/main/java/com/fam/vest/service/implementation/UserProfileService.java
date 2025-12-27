package com.fam.vest.service.implementation;

import com.fam.vest.dto.request.ChangePasswordRequest;
import com.fam.vest.dto.request.UpdateProfileRequest;
import com.fam.vest.dto.response.ApplicationUserDto;
import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.service.IUserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService implements IUserProfileService {

    private static final String USER_NOT_FOUND = "User not found";
    private static final String USERNAME_EXISTS = "Username already exists";
    private static final String PASSWORD_MISMATCH = "New password and confirm password do not match";
    private static final String INCORRECT_PASSWORD = "Old password is incorrect";

    private final ApplicationUserRepository applicationUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ApplicationUserDto getUserProfile(UserDetails userDetails) {
        ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userDetails.getUsername());
        if (applicationUser == null) {
            throw new IllegalArgumentException(USER_NOT_FOUND);
        }
        ApplicationUserDto dto = new ApplicationUserDto();
        dto.setId(applicationUser.getId());
        dto.setFullName(applicationUser.getFullName());
        dto.setUserName(applicationUser.getUserName());
        dto.setRole(applicationUser.getRole());
        dto.setCreatedAt(applicationUser.getCreatedDate());
        dto.setUpdatedAt(applicationUser.getLastModifiedDate());

        return dto;
    }

    @Override
    public ApplicationUserDto updateProfile(UserDetails userDetails, UpdateProfileRequest request) {
        ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userDetails.getUsername());
        if (applicationUser == null) {
            throw new IllegalArgumentException(USER_NOT_FOUND);
        }
        // Check if new username already exists (if username is being changed)
        if (request.getUserName() != null && !request.getUserName().equals(applicationUser.getUserName())) {
            ApplicationUser existingUser = applicationUserRepository.findApplicationUserByUserName(request.getUserName());
            if (existingUser != null) {
                log.warn("Username already exists: {}", request.getUserName());
                throw new IllegalArgumentException(USERNAME_EXISTS);
            }
            applicationUser.setUserName(request.getUserName());
        }
        // Update full name if provided
        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            applicationUser.setFullName(request.getFullName());
        }
        applicationUser.setLastModifiedDate(new Date());
        applicationUser.setLastModifiedBy(userDetails.getUsername());
        ApplicationUser savedUser = applicationUserRepository.save(applicationUser);
        log.info("Profile updated successfully for user {}", userDetails.getUsername());
        // Build response
        ApplicationUserDto response = new ApplicationUserDto();
        response.setId(savedUser.getId());
        response.setFullName(savedUser.getFullName());
        response.setUserName(savedUser.getUserName());
        response.setRole(savedUser.getRole());
        response.setCreatedAt(savedUser.getCreatedDate());
        response.setUpdatedAt(savedUser.getLastModifiedDate());

        return response;
    }

    @Override
    public String changePassword(UserDetails userDetails, ChangePasswordRequest request) {
        ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userDetails.getUsername());
        if (applicationUser == null) {
            throw new IllegalArgumentException(USER_NOT_FOUND);
        }
        // Validate new password and confirm password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Passwords do not match for user {}", userDetails.getUsername());
            throw new IllegalArgumentException(PASSWORD_MISMATCH);
        }
        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), applicationUser.getPassword())) {
            log.warn("Incorrect old password provided by user {}", userDetails.getUsername());
            throw new IllegalArgumentException(INCORRECT_PASSWORD);
        }
        // Update password
        applicationUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        applicationUser.setLastModifiedDate(new Date());
        applicationUser.setLastModifiedBy(userDetails.getUsername());

        applicationUserRepository.save(applicationUser);
        log.info("Password changed successfully for user {}", userDetails.getUsername());
        return "Password changed successfully";
    }
}

