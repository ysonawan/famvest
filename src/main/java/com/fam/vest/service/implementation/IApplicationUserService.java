package com.fam.vest.service.implementation;

import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.exception.UserNotFoundException;
import com.fam.vest.repository.ApplicationUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class IApplicationUserService implements UserDetailsService {

    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    public IApplicationUserService(ApplicationUserRepository applicationUserRepository) {
        this.applicationUserRepository = applicationUserRepository;
    }

    @Override
    public User loadUserByUsername(String userName) throws UserNotFoundException {
        ApplicationUser applicationUser =  applicationUserRepository.findApplicationUserByUserName(userName);
        if(applicationUser == null) {
            throw new UserNotFoundException("User not found with email: " + userName);
        }
        return new org.springframework.security.core.userdetails.User(
                applicationUser.getUserName(),
                applicationUser.getPassword(), // no {noop}, use actual hashed password
                Collections.singleton(new SimpleGrantedAuthority(applicationUser.getRole()))
        );
    }

}
