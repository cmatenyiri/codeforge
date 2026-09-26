package com.codeforge.service;

import java.util.concurrent.Executor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Where a judged request carries on once the sandbox has answered.
 *
 * <p>Judging is asynchronous: the request thread queues the work and is let go,
 * and the results arrive later on whichever thread delivered Judge0's callback.
 * What happens next — comparing outputs, recording the submission, scoring it —
 * must neither run on that thread, which has other callbacks to deliver, nor
 * run as nobody. Everything downstream is behind {@code @PreAuthorize} and reads
 * the caller from the SecurityContext, so it has to run as the person who asked.
 *
 * <p>Virtual threads, because the work is a few short database writes and there
 * is no reason to size a pool for how many verdicts land at once.
 */
@Component
public class ContinuationExecutor {

    private final SimpleAsyncTaskExecutor executor;

    public ContinuationExecutor() {
        executor = new SimpleAsyncTaskExecutor("judged-");
        executor.setVirtualThreads(true);
    }

    /**
     * An executor that runs its tasks as the current caller.
     *
     * <p>Call it on the request thread: the SecurityContext is captured here, at
     * the moment of the call, not when the task eventually runs.
     */
    public Executor asCurrentCaller() {
        return new DelegatingSecurityContextExecutor(executor, SecurityContextHolder.getContext());
    }
}
