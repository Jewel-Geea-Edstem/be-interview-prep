package com.example.prep.user.controller;

import com.example.prep.user.dto.response.UserResponse;
import com.example.prep.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
    return userService.getByEmail(jwt.getSubject());
  }

  @GetMapping
  public PagedModel<UserResponse> list(@PageableDefault(sort = "id") Pageable pageable) {
    return new PagedModel<>(userService.list(pageable));
  }
}
