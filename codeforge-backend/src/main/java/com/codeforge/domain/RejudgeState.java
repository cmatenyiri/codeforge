package com.codeforge.domain;

/**
 * Progress of a contest's last rejudge.
 *
 * <p>Recorded on the contest rather than kept in memory because a rejudge
 * outlives the request that asked for it — it re-runs every submission of every
 * participant through the sandbox, which is minutes of work — and the authoring
 * screen has to be able to come back and ask how it is going.
 */
public enum RejudgeState {

    /** Never rejudged. */
    NONE,

    RUNNING,

    COMPLETED,

    /** Stopped on an error; the contest is left with whatever it had judged so far. */
    FAILED
}
