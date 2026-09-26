package com.codeforge.realtime;

/**
 * Something about a contest changed that a screen showing it should re-read.
 *
 * <p>Raised by the services as an application event and pushed to the browser
 * by {@link LiveUpdates} once the change has committed. It is also, unchanged,
 * the message the browser receives.
 *
 * @param change what kind of thing changed, so each screen can ignore what it
 *     does not show
 */
public record ContestChanged(Long contestId, Change change) {

    public enum Change {
        /**
         * The contest's clock or settings: it started or ended, was announced,
         * withdrawn, edited, deleted, settled or had its rating withdrawn.
         */
        STATUS,

        /** Somebody registered or withdrew their registration. */
        REGISTRATION,

        /** The scoreboard moved: a counted verdict landed, or it was rebuilt. */
        STANDINGS,

        /** A rejudge started, progressed or finished. Only authors hear about it. */
        REJUDGE
    }
}
