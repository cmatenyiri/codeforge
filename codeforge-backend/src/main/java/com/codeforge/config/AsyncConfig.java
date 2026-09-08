package com.codeforge.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

/**
 * The one background worker, for jobs that outlive the request that asked for
 * them.
 *
 * <p>At present that means one thing: re-judging a contest, which re-runs every
 * submission every competitor made through the sandbox. That is minutes of work
 * on a busy contest — far past any HTTP timeout — so the request starts it and
 * returns, and the authoring screen polls the progress recorded on the contest.
 *
 * <p>A single thread, deliberately. Two rejudges at once would compete for the
 * sandbox and finish later than if they had queued, and two rejudges of the
 * <em>same</em> contest would race to rebuild the same standings. Queueing them
 * makes both impossible without any locking.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Wrapped in a {@link DelegatingSecurityContextAsyncTaskExecutor} so that the
     * caller's authentication travels onto the worker thread.
     *
     * <p>Without it, the {@code @PreAuthorize("hasRole('ADMIN')")} annotations on
     * everything a rejudge touches would see an empty SecurityContext and refuse
     * — the job would fail on its first database call, having already told the
     * admin it had started. The alternative, dropping those annotations for the
     * background path, would mean the most destructive operation in the
     * application is the one nothing checks.
     */
    @Bean("contestTaskExecutor")
    public Executor contestTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("contest-rejudge-");
        // Long jobs get to finish rather than being cut off by a redeploy.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
