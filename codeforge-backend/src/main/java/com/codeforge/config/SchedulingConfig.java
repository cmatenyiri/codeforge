package com.codeforge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled}, for the one job that has to run whether anybody
 * is looking or not: publishing a finished contest's problems — see
 * {@link com.codeforge.service.ContestReleaseJob}.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
