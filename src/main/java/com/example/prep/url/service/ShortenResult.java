package com.example.prep.url.service;

import com.example.prep.url.dto.response.ShortUrlResponse;

public record ShortenResult(ShortUrlResponse shortUrl, boolean created) {}
