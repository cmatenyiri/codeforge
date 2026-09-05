package com.codeforge.config;

import com.codeforge.CodeforgeApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/**
 * Discovers every {@code @ConfigurationProperties} type by scanning.
 *
 * <p>Adding a new properties record is then a single annotation on the record
 * itself — no second edit to keep a registration list in sync, and no chance of
 * a binding silently going missing because someone forgot one.
 *
 * <p>{@code basePackageClasses} is required because the scan otherwise starts
 * from this class's own package: without it only {@code com.codeforge.config}
 * would be searched, and {@code com.codeforge.security.JwtProperties} would be
 * missed. Naming the application class rather than a string keeps that anchored
 * through a package rename.
 */
@Configuration
@ConfigurationPropertiesScan(basePackageClasses = CodeforgeApplication.class)
public class PropertiesConfig {}
