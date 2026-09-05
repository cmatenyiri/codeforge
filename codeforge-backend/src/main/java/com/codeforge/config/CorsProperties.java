package com.codeforge.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from {@code codeforge.cors.*}. */
@ConfigurationProperties(prefix = "codeforge.cors")
public record CorsProperties(List<String> allowedOrigins) {}
