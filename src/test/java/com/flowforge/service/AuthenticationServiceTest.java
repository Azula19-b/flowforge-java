package com.flowforge.service;

import com.flowforge.dto.auth.AuthResponse;
import com.flowforge.dto.auth.RegisterRequest;
import com.flowforge.entity.User;
import com.flowforge.enums.Role;
import com.flowforge.exception.DuplicateResourceException;
import com.flowforge.repository.UserRepository;
import com.flowforge.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private JwtService jwtService;
    @Mock private UserDetails userDetails;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(userRepository, passwordEncoder,
                authenticationManager, userDetailsService, jwtService);
    }

    @Test
    void registrationNormalizesEmailHashesPasswordAndAssignsMemberRole() {
        RegisterRequest request = new RegisterRequest(" Ada ", " Lovelace ",
                " ADA@Example.COM ", "strong-pass");
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("strong-pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
            return user;
        });
        when(userDetailsService.loadUserByUsername("ada@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("signed-token");
        when(jwtService.extractExpiration("signed-token"))
                .thenReturn(Instant.parse("2026-01-02T00:00:00Z"));

        AuthResponse response = service.register(request);

        assertThat(response.user().email()).isEqualTo("ada@example.com");
        assertThat(response.user().role()).isEqualTo(Role.MEMBER);
        assertThat(response.token()).isEqualTo("signed-token");
        verify(passwordEncoder).encode("strong-pass");
    }

    @Test
    void duplicateEmailIsRejectedBeforePasswordIsProcessed() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "Ada", "Lovelace", "ADA@example.com", "strong-pass")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }
}
