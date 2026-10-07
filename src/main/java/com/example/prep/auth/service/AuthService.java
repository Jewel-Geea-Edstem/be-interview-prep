package com.example.prep.auth.service;

import com.example.prep.auth.dto.request.LoginRequest;
import com.example.prep.auth.dto.request.RegisterRequest;
import com.example.prep.auth.dto.response.TokenResponse;
import com.example.prep.user.dto.response.UserResponse;
import com.example.prep.user.entity.Role;
import com.example.prep.user.entity.User;
import com.example.prep.user.repository.UserRepository;
import com.example.prep.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserService userService;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final String unknownUserHash;

  public AuthService(
      UserService userService,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService) {
    this.userService = userService;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  public UserResponse register(RegisterRequest request) {
    return userService.create(request.email(), request.password(), Role.USER);
  }

  @Transactional(readOnly = true)
  public TokenResponse login(LoginRequest request) {
    Optional<User> user = userRepository.findByEmail(UserService.normalizeEmail(request.email()));
    String hash = user.map(User::getPasswordHash).orElse(unknownUserHash);
    boolean matches = passwordEncoder.matches(request.password(), hash);
    if (user.isEmpty() || !matches) {
      throw new BadCredentialsException("Invalid email or password");
    }
    return tokenService.issue(user.get());
  }
}
