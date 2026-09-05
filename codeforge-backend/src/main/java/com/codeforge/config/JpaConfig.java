package com.codeforge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on the auditing listener that populates {@code createdAt} and
 * {@code updatedAt} on {@link com.codeforge.domain.BaseEntity}.
 *
 * <p>No {@code AuditorAware} bean is registered: only the timestamps are
 * audited, not who made the change. Add one here if {@code @CreatedBy} is ever
 * needed — note it would read the SecurityContext, so background writes outside
 * a request would need to supply an auditor of their own.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
