package com.codeforge.execution.judge0;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * The batches this instance has sent to Judge0 and is still waiting on.
 *
 * <p>Nothing here asks Judge0 anything. A batch is opened before it is sent,
 * each of its submissions carries a callback naming it, and the batch completes
 * when the last of those callbacks has been {@linkplain #deliver delivered} —
 * by {@link Judge0CallbackController} when Judge0 called this instance, or by
 * {@link Judge0CallbackRouter} when it called another one.
 *
 * <p>Held in memory, and deliberately so: what waits on a batch is a request
 * this instance is holding open, and that request does not outlive the instance
 * either.
 */
@Component
class Judge0Batches {

    private final Map<String, Batch> waiting = new ConcurrentHashMap<>();

    /**
     * Starts waiting on a batch that is about to be sent.
     *
     * <p>Opened first, not after Judge0 has answered with the tokens: a short
     * program can finish, and its callback arrive, before the response to the
     * create call has even been read.
     */
    Batch open() {
        Batch batch = new Batch(UUID.randomUUID().toString());
        waiting.put(batch.key(), batch);
        // However it ends — every callback in, a timeout, a failed send — it is
        // no longer waited on, and a callback that turns up later is dropped.
        batch.future().whenComplete((results, failure) -> waiting.remove(batch.key()));
        return batch;
    }

    /**
     * Hands one finished submission to the batch it belongs to.
     *
     * <p>A key that is not waiting here is not an error: the batch timed out
     * before a slow callback arrived, or the key was never this instance's.
     * Either way there is nobody left to tell.
     */
    void deliver(String key, Judge0Api.Result result) {
        Batch batch = waiting.get(key);
        if (batch != null) {
            batch.deliver(result);
        }
    }

    /**
     * One batch's callbacks, gathered until every submission in it has reported.
     *
     * <p>The key is random and is what the callback URL carries, so it doubles
     * as the credential: a result can only land in a batch whose key the sender
     * was given, and only Judge0 was ever given it.
     */
    static final class Batch {

        private final String key;
        private final CompletableFuture<List<Judge0Api.Result>> future = new CompletableFuture<>();
        private final Map<String, Judge0Api.Result> arrived = new HashMap<>();

        /** Null until Judge0 has answered the create call; callbacks may beat it. */
        private List<String> tokens;

        private Batch(String key) {
            this.key = key;
        }

        String key() {
            return key;
        }

        /** Completes with one result per token, in the order the tokens were issued. */
        CompletableFuture<List<Judge0Api.Result>> future() {
            return future;
        }

        /** The tokens Judge0 issued, in submission order. */
        void expect(List<String> issued) {
            List<Judge0Api.Result> settled;
            synchronized (this) {
                tokens = List.copyOf(issued);
                settled = settled();
            }
            // Completed outside the lock: whatever is chained on the future runs
            // on this thread, and none of it needs the lock held.
            if (settled != null) {
                future.complete(settled);
            }
        }

        void deliver(Judge0Api.Result result) {
            if (result.token() == null
                    || result.status() == null
                    || Judge0Verdict.isPending(result.status().id())) {
                return;
            }
            List<Judge0Api.Result> settled;
            synchronized (this) {
                arrived.put(result.token(), result);
                settled = settled();
            }
            if (settled != null) {
                future.complete(settled);
            }
        }

        synchronized List<String> tokens() {
            return tokens == null ? List.of() : tokens;
        }

        private List<Judge0Api.Result> settled() {
            if (tokens == null || !arrived.keySet().containsAll(tokens)) {
                return null;
            }
            return tokens.stream().map(arrived::get).toList();
        }
    }
}
