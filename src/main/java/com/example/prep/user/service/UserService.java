package com.example.prep.user.service;

import com.example.prep.common.exception.ConflictException;
import com.example.prep.common.exception.NotFoundException;
import com.example.prep.user.dto.response.UserResponse;
import com.example.prep.user.entity.Role;
import com.example.prep.user.entity.User;
import com.example.prep.user.mapper.UserMapper;
import com.example.prep.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  public static String normalizeEmail(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  @Transactional
  public UserResponse create(String email, String rawPassword, Role role) {
    String normalized = normalizeEmail(email);
    if (userRepository.existsByEmail(normalized)) {
      throw new ConflictException("Email " + normalized + " is already registered");
    }
    User user = new User();
    user.setEmail(normalized);
    user.setPasswordHash(passwordEncoder.encode(rawPassword));
    user.setRole(role);
    return userMapper.toResponse(userRepository.save(user));
  }

  @Transactional(readOnly = true)
  public boolean exists(String email) {
    return userRepository.existsByEmail(normalizeEmail(email));
  }

  @Transactional(readOnly = true)
  public UserResponse getByEmail(String email) {
    return userRepository
        .findByEmail(normalizeEmail(email))
        .map(userMapper::toResponse)
        .orElseThrow(() -> new NotFoundException("User", email));
  }

  @Transactional(readOnly = true)
  public Page<UserResponse> list(Pageable pageable) {
    return userRepository.findAll(pageable).map(userMapper::toResponse);
  }
}
