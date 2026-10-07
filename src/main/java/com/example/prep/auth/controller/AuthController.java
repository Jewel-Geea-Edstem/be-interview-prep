package com.example.prep.auth.controller;

import com.example.prep.auth.dto.request.LoginRequest;
import com.example.prep.auth.dto.request.RegisterRequest;
import com.example.prep.auth.dto.response.TokenResponse;
import com.example.prep.auth.service.AuthService;
import com.example.prep.user.dto.response.UserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
    UserResponse created = authService.register(request);
    return ResponseEntity.created(URI.create("/api/v1/users/me")).body(created);
  }

  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
