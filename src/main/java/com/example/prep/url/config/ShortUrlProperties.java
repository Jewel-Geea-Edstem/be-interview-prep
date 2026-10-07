package com.example.prep.url.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.short-url")
public record ShortUrlProperties(String baseUrl) {}
