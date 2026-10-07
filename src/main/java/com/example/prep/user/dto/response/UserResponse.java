package com.example.prep.user.dto.response;

import com.example.prep.user.entity.Role;
import java.time.Instant;

public record UserResponse(Long id, String email, Role role, Instant createdAt) {}
