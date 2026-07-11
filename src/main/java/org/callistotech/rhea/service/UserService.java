package org.callistotech.rhea.service;

import org.callistotech.rhea.model.AppUser;
import org.callistotech.rhea.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUserName(userName.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No account found for: " + userName));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserName())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .disabled(!user.isActive())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    public AppUser registerLocal(String name, String email, String rawPassword) {
        String userName = email.toLowerCase().trim();
        if (userRepository.existsByUserName(userName)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        AppUser user = new AppUser();
        user.setUserName(userName);
        user.setName(name.trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setProvider("local");
        return userRepository.save(user);
    }

    public AppUser findOrCreateGoogleUser(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        if (email == null) {
            throw new IllegalStateException("Google account has no email address.");
        }

        String userName = email.toLowerCase().trim();
        Optional<AppUser> existing = userRepository.findByUserName(userName);
        if (existing.isPresent()) {
            return existing.get();
        }

        AppUser user = new AppUser();
        user.setUserName(userName);
        user.setName(name != null ? name : email);
        user.setProvider("google");
        return userRepository.save(user);
    }

    public Optional<AppUser> findByUserName(String userName) {
        return userRepository.findByUserName(userName.toLowerCase());
    }
}
