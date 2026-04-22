package com.lms.iam.security;

import com.lms.iam.model.User;
import com.lms.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Get User by email
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Not found user with email: " + email));
        // Get permissions of user
        Set<String> permissions = userRepository.findPermissionKeysByUserId(user.getId());

        return new CustomUserDetails(user, permissions);
    }
}
