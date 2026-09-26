package com.codeforge.service;

import com.codeforge.domain.Contest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Component;

/**
 * Wakes a contest up at its start and at its end, whether anybody is looking or
 * not.
 *
 * <p>Two moments in a contest's life have work attached that cannot wait for a
 * visitor: the start, when its problems are frozen for the whole field, and the
 * end, when they go into the public catalogue. Every announced contest gets a
 * one-shot Quartz trigger for each, at exactly that instant — so a contest
 * nobody opens is sealed at 10:00:00 all the same, and nothing polls in between.
 *
 * <h2>Stored with the contest, and fired late rather than never</h2>
 *
 * <p>Quartz keeps the triggers in its own tables in the same database, and
 * scheduling joins the caller's transaction: the alarm is saved, moved or
 * cleared in the same commit as the contest change that called for it, and
 * never exists for a change that rolled back.
 *
 * <p>A trigger whose moment passes while the server is down is still in those
 * tables when it comes back, and Quartz fires it straight away — so a contest
 * that starts during a restart is sealed the moment the scheduler is up, rather
 * than whenever somebody next opens it.
 *
 * <p>An alarm decides nothing, either. When it goes off it calls the same
 * {@link ContestService#sealIfDue} and {@link ContestService#releaseIfDue} a page
 * load does, which re-check everything under the contest's row lock; one that
 * goes off for a contest that has since been withdrawn does nothing.
 */
@Component
@RequiredArgsConstructor
public class ContestAlarms {

    private static final String GROUP = "contest-alarms";

    /** How long a seal or release that failed waits before it is tried again. */
    private static final Duration RETRY_DELAY = Duration.ofMinutes(1);

    private final Scheduler scheduler;

    /** The two moments a contest has work attached to. */
    public enum Moment {
        /** Freeze the problems. */
        START,
        /** Release them into the catalogue. */
        END
    }

    /**
     * Sets both alarms to match the contest as it now stands.
     *
     * <p>Called from inside the transaction that changed the contest, which the
     * trigger writes join — until it commits, the scheduler cannot see them.
     */
    public void arm(Contest contest) {
        set(contest.getId(),
                Moment.START,
                contest.isPublished() && !contest.isSealed() ? contest.getStartsAt() : null);
        set(contest.getId(), Moment.END, contest.isHoldingProblems() ? contest.getEndsAt() : null);
    }

    /** Clears both alarms of a contest that is being deleted. */
    public void disarm(Long contestId) {
        set(contestId, Moment.START, null);
        set(contestId, Moment.END, null);
    }

    /** Sets one alarm again for a minute from now, after the work it did failed. */
    void retry(Long contestId, Moment moment) {
        set(contestId, moment, Instant.now().plus(RETRY_DELAY));
    }

    /**
     * Schedules one alarm, replacing whatever was set for it before, or clears
     * it when {@code at} is null.
     */
    private void set(Long contestId, Moment moment, Instant at) {
        JobKey key = JobKey.jobKey(contestId + "-" + moment.name().toLowerCase(Locale.ROOT), GROUP);
        try {
            if (at == null) {
                scheduler.deleteJob(key);
                return;
            }

            JobDetail job = JobBuilder.newJob(ContestAlarmJob.class)
                    .withIdentity(key)
                    .usingJobData(ContestAlarmJob.CONTEST_ID, contestId)
                    .usingJobData(ContestAlarmJob.MOMENT, moment.name())
                    // Run again after a crash that cut it off part way. Sealing
                    // and releasing are both safe to repeat.
                    .requestRecovery()
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(key.getName(), GROUP)
                    .forJob(job)
                    // Rounded up, never down: a Date only holds milliseconds, and
                    // an alarm a fraction early would find the contest not due.
                    .startAt(Date.from(ceilToMillis(at)))
                    // Missed while the server was down: fire as soon as it is up.
                    // Already what Quartz does with a one-shot trigger by default,
                    // stated because the whole design leans on it.
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                    .build();
            scheduler.scheduleJob(job, Set.of(trigger), true);
        } catch (SchedulerException e) {
            // Thrown on, so the change that called for this alarm rolls back with
            // it rather than committing a contest that nothing will wake.
            throw new IllegalStateException("Could not set the " + moment + " alarm of contest " + contestId, e);
        }
    }

    private static Instant ceilToMillis(Instant instant) {
        Instant truncated = instant.truncatedTo(ChronoUnit.MILLIS);
        return truncated.equals(instant) ? instant : truncated.plusMillis(1);
    }
}
