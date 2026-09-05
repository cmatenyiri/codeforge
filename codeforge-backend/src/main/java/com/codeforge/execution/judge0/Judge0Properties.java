package com.codeforge.execution.judge0;

import com.codeforge.domain.Language;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Judge0 settings, bound from {@code codeforge.execution.judge0.*}.
 *
 * @param baseUrl root of the Judge0 API, e.g. {@code http://localhost:2358}
 * @param authToken value for {@code X-Auth-Token}; null when the instance is
 *     unauthenticated, which is only acceptable on a private network
 * @param languageIds our {@link Language} to Judge0's numeric language id. These
 *     are instance-specific — {@code GET /languages} lists what a given image
 *     actually has — so they are configuration rather than constants
 * @param cpuTimeLimit per-case CPU budget; Judge0 rejects anything above its own
 *     {@code MAX_CPU_TIME_LIMIT}, 15s by default
 * @param wallTimeLimit per-case wall-clock budget, capped at 20s by default
 * @param memoryLimitKb per-case address-space cap
 * @param pollInterval how often to ask whether a batch has finished
 * @param pollTimeout how long to keep asking before giving up on the batch
 * @param connectTimeout TCP connect budget for one call to Judge0
 * @param readTimeout budget for one call's response. Each call is short — the
 *     waiting happens across polls, not inside one — so this stays well below
 *     {@code pollTimeout}
 */
@ConfigurationProperties(prefix = "codeforge.execution.judge0")
public record Judge0Properties(
        String baseUrl,
        String authToken,
        Map<Language, Integer> languageIds,
        Duration cpuTimeLimit,
        Duration wallTimeLimit,
        int memoryLimitKb,
        Duration pollInterval,
        Duration pollTimeout,
        Duration connectTimeout,
        Duration readTimeout) {

    /** Judge0 takes limits as fractional seconds. */
    public double cpuTimeLimitSeconds() {
        return cpuTimeLimit.toMillis() / 1000.0;
    }

    public double wallTimeLimitSeconds() {
        return wallTimeLimit.toMillis() / 1000.0;
    }
}
