package com.codeforge.service;

import com.codeforge.repository.ContestRepository;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publishes the problems of contests that have ended.
 *
 * <p>The one thing about a contest that cannot wait for somebody to open it.
 * Starting and ending need no job — both are derived from the clock on every
 * read — but the catalogue only lists problems whose {@code published} flag is
 * set, so a finished contest that nobody visits would otherwise keep its
 * problems out of it indefinitely. The first visit after the end releases them
 * immediately; this makes sure that happens within a minute either way.
 */
@Component
@RequiredArgsConstructor
public class ContestReleaseJob {

    private static final Logger log = LoggerFactory.getLogger(ContestReleaseJob.class);

    private final ContestRepository contestRepository;
    private final ContestService contestService;

    @Scheduled(fixedDelay = 60, initialDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void releaseEndedContests() {
        for (Long contestId : contestRepository.findDueForRelease(Instant.now())) {
            // One transaction per contest, through the proxy, so that one that
            // fails is retried on the next pass without holding up the rest.
            try {
                contestService.releaseIfDue(contestId);
            } catch (RuntimeException e) {
                log.error("Could not release the problems of contest {}", contestId, e);
            }
        }
    }
}
