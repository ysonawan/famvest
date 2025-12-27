package com.fam.vest.service.implementation;

import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.UserPreferences;
import com.fam.vest.enums.DEFAULT_USER_PREFERENCES;
import com.fam.vest.exception.ResourceNotFoundException;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.repository.UserPreferencesRepository;
import com.fam.vest.service.UserPreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IUserPreferencesService implements UserPreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;
    private final ApplicationUserRepository applicationUserRepository;

    @Override
    public List<UserPreferences> getUserPreferences(String userName) {
        ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userName);
        if(null == applicationUser) {
            throw new ResourceNotFoundException("User with username " + userName + " not found");
        }
        List<UserPreferences> userPreferences = userPreferencesRepository.getUserPreferencesByUserIdOrderByPreferenceAsc(applicationUser.getId());
        List<DEFAULT_USER_PREFERENCES> defaultUserPreferences = DEFAULT_USER_PREFERENCES.getDefaultPreferences();
        Date currentTime = Calendar.getInstance().getTime();

        // Add missing default preferences
        for (DEFAULT_USER_PREFERENCES defaultPref : defaultUserPreferences) {
            boolean exists = userPreferences.stream()
                .anyMatch(up -> up.getPreference() == defaultPref);
            if (!exists) {
                log.info("Default preference {} not found for user {}. Adding it.", defaultPref, userName);
                UserPreferences userPreference = new UserPreferences();
                userPreference.setUserId(applicationUser.getId());
                userPreference.setPreference(defaultPref);
                userPreference.setValue(defaultPref.getDefaultValue());
                userPreference.setCreatedBy("System");
                userPreference.setCreatedDate(currentTime);
                userPreference.setLastModifiedBy("System");
                userPreference.setLastModifiedDate(currentTime);
                userPreferencesRepository.save(userPreference);
            }
        }

        // Delete preferences not in default
        for (UserPreferences userPref : userPreferences) {
            boolean isDefault = defaultUserPreferences.stream()
                .anyMatch(defPref -> defPref == userPref.getPreference());
            if (!isDefault) {
                log.info("Saved preference {} not in default list. Deleting user preference id {}", userPref.getPreference(), userPref.getId());
                userPreferencesRepository.delete(userPref);
            }
        }

        userPreferences = userPreferencesRepository.getUserPreferencesByUserIdOrderByPreferenceAsc(applicationUser.getId());
        userPreferences.forEach(userPreference -> {
            userPreference.setDescription(userPreference.getPreference().getDescription());
            userPreference.setDisplayName(userPreference.getPreference().getDisplayName());
            userPreference.setAllowedValues(userPreference.getPreference().getAllowedValues());
        });
        return userPreferences;
    }

    @Override
    public UserPreferences updateUserPreferenceValue(Long id, String value, String userName) {
        ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userName);
        if(null == applicationUser) {
            throw new ResourceNotFoundException("User with username " + userName + " not found");
        }
        UserPreferences userPreference = userPreferencesRepository.getUserPreferencesByIdAndUserId(id, applicationUser.getId()).
                orElseThrow(() -> new ResourceNotFoundException("User preference with id " + id + " not found for user " + userName));
        userPreference.setValue(value);
        userPreference.setLastModifiedBy(userName);
        userPreference.setLastModifiedDate(Calendar.getInstance().getTime());
        return userPreferencesRepository.save(userPreference);
    }
}
