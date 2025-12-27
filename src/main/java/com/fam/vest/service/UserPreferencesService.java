package com.fam.vest.service;

import com.fam.vest.entity.UserPreferences;

import java.util.List;

public interface UserPreferencesService {

    List<UserPreferences> getUserPreferences(String userName);

    UserPreferences updateUserPreferenceValue(Long id, String value, String userName);
}
