package com.flowforge.service;

import com.flowforge.dto.user.UserResponse;
import com.flowforge.entity.User;
import com.flowforge.enums.Role;
import com.flowforge.exception.ResourceNotFoundException;
import com.flowforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAllByOrderByFirstNameAscLastNameAsc().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + userId + " was not found"));
        user.setRole(role);
        return UserResponse.from(userRepository.save(user));
    }
}
