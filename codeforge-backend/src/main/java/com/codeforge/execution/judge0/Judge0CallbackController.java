package com.codeforge.execution.judge0;

import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Where Judge0 reports a finished submission.
 *
 * <p>Lives with the engine rather than with the application's controllers: it
 * is part of how this backend talks to its sandbox, not something the frontend
 * calls. It is open to unauthenticated callers, because Judge0 cannot sign in —
 * what protects it is that a result only counts when it names a batch that is
 * being waited on, and those keys are random and handed to nobody but Judge0.
 *
 * <p>Answers at once and does no judging of its own. Judge0 gives a callback a
 * few seconds before it retries; the work the result sets off happens on
 * whatever is waiting for it.
 */
@RestController
@RequestMapping("/api/judge0/callbacks")
class Judge0CallbackController {

    /** Instance ids and batch keys are both UUIDs; anything else is not ours. */
    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final Judge0CallbackRouter router;

    Judge0CallbackController(Judge0CallbackRouter router) {
        this.router = router;
    }

    @PutMapping("/{instance}/{batch}")
    ResponseEntity<Void> callback(
            @PathVariable String instance, @PathVariable String batch, @RequestBody byte[] body) {

        if (!UUID_PATTERN.matcher(instance).matches() || !UUID_PATTERN.matcher(batch).matches()) {
            return ResponseEntity.notFound().build();
        }
        router.route(instance, batch, body);
        return ResponseEntity.noContent().build();
    }
}
