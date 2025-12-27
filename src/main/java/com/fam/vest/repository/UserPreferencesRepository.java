package com.fam.vest.repository;

import com.fam.vest.entity.UserPreferences;
import com.fam.vest.enums.DEFAULT_USER_PREFERENCES;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    List<UserPreferences> getUserPreferencesByUserIdOrderByPreferenceAsc(Long userId);
    Optional<UserPreferences> getUserPreferencesByIdAndUserId(Long id, Long userId);
    Optional<UserPreferences> getUserPreferencesByPreferenceAndUserId(DEFAULT_USER_PREFERENCES userPreferences, Long userId);
}
