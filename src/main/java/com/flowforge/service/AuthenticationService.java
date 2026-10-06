package com.flowforge.service;

import com.flowforge.dto.auth.AuthResponse;
import com.flowforge.dto.auth.LoginRequest;
import com.flowforge.dto.auth.RegisterRequest;
import com.flowforge.dto.user.UserResponse;
import com.flowforge.entity.User;
import com.flowforge.enums.Role;
import com.flowforge.exception.DuplicateResourceException;
import com.flowforge.repository.UserRepository;
import com.flowforge.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account already exists for " + email);
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.MEMBER);
        user = userRepository.save(user);

        return issueToken(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                        "Invalid credentials"));
        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(details);
        return new AuthResponse(token, "Bearer", jwtService.extractExpiration(token), UserResponse.from(user));
    }
}
