package com.fam.vest.repository;

import com.fam.vest.entity.ApplicationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {

    ApplicationUser findApplicationUserByUserName(String userName);

    List<ApplicationUser> findApplicationUserByRole(String role);
}
