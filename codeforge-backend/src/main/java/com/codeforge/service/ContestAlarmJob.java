package com.codeforge.service;

import com.codeforge.service.ContestAlarms.Moment;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What an alarm set by {@link ContestAlarms} does when it goes off: seal the
 * contest at its start, or release its problems at its end.
 *
 * <p>Not a bean. Quartz asks Spring for a fresh instance on every firing, which
 * is what lets the constructor be injected like any other.
 */
@RequiredArgsConstructor
public class ContestAlarmJob implements Job {

    static final String CONTEST_ID = "contestId";
    static final String MOMENT = "moment";

    private static final Logger log = LoggerFactory.getLogger(ContestAlarmJob.class);

    private final ContestService contestService;
    private final ContestAlarms alarms;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap data = context.getMergedJobDataMap();
        Long contestId = data.getLong(CONTEST_ID);
        Moment moment = Moment.valueOf(data.getString(MOMENT));

        waitUntil(context.getScheduledFireTime());
        // Through the proxy, so each runs in its own transaction and under the
        // contest's row lock — the same calls, and the same race, as a page load.
        try {
            switch (moment) {
                case START -> contestService.sealIfDue(contestId);
                case END -> contestService.releaseIfDue(contestId);
            }
        } catch (RuntimeException e) {
            log.error("The {} alarm of contest {} failed; trying again shortly", moment, contestId, e);
            alarms.retry(contestId, moment);
        }
    }

    /**
     * Holds the firing back to the moment it was set for.
     *
     * <p>Quartz releases a trigger up to a couple of milliseconds ahead of its
     * time, and a seal that ran then would find the contest not started yet and
     * do nothing — leaving it to the first visitor, which is the very thing the
     * alarm exists to prevent.
     */
    private static void waitUntil(Date scheduled) {
        long early = scheduled.getTime() - System.currentTimeMillis();
        if (early <= 0) {
            return;
        }
        try {
            Thread.sleep(early);
        } catch (InterruptedException e) {
            // Carried on regardless: returning would count as having run, and
            // the trigger would be gone with nothing sealed.
            Thread.currentThread().interrupt();
        }
    }
}
