package com.example.prep.user.mapper;

import com.example.prep.user.dto.response.UserResponse;
import com.example.prep.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserResponse toResponse(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
  }
}
