package com.example.prep.url.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ShortUrlProperties.class)
public class ShortUrlConfig {}
