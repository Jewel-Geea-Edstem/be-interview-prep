package com.example.prep.auth.dto.response;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
