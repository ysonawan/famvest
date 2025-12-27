package com.fam.vest.controller;

import com.fam.vest.entity.UserPreferences;
import com.fam.vest.service.UserPreferencesService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.UserDetailsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rest/v1/user-preferences")
@CrossOrigin
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    @GetMapping()
    public ResponseEntity<Object> getUserPreferences() {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching user preferences for {}", userDetails.getUsername());
        List<UserPreferences> userPreferences = userPreferencesService.getUserPreferences(userDetails.getUsername());
        return CommonUtil.success(userPreferences);
    }

    @PatchMapping("/{id}/value")
    public ResponseEntity<Object> updateUserPreferenceValue(@PathVariable Long id, @RequestBody String value) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Updating user preferences for id {} by {}", id, userDetails.getUsername());
        UserPreferences userPreference = userPreferencesService.updateUserPreferenceValue(id, value, userDetails.getUsername());
        return CommonUtil.success(userPreference, "User preference value updated successfully.");
    }

}
