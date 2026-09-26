package com.codeforge.realtime;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Pushes the services' change events to the browsers watching.
 *
 * <p>After commit, never before: a browser told that the standings moved
 * re-reads them at once, and a message that beat the commit would send it to
 * read the old ones — and then nothing would tell it again. A change made
 * outside any transaction is pushed straight away.
 *
 * <p>What is sent goes to RabbitMQ, which fans it out to every subscriber
 * through whichever instance each of them is connected to.
 *
 * <p>Best effort. The change has already been committed when this runs, so a
 * broker that cannot be reached must not turn it into a failed request; the
 * browsers re-read everything when their connection comes back anyway.
 */
@Component
@RequiredArgsConstructor
class LiveUpdates {

    private static final Logger log = LoggerFactory.getLogger(LiveUpdates.class);

    private final SimpMessageSendingOperations messaging;

    @TransactionalEventListener(fallbackExecution = true)
    void on(ContestChanged event) {
        switch (event.change()) {
            // The lobby shows every announced contest's clock and turnout, but
            // not the scoreboards, which move with every counted verdict.
            case STATUS, REGISTRATION -> {
                send(Topics.contest(event.contestId()), event);
                send(Topics.CONTESTS, event);
            }
            case STANDINGS -> send(Topics.contest(event.contestId()), event);
            case REJUDGE -> send(Topics.adminContest(event.contestId()), event);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(InterviewChanged event) {
        send(Topics.interview(event.interviewId()), event);
    }

    private void send(String destination, Object payload) {
        try {
            messaging.convertAndSend(destination, payload);
        } catch (MessagingException e) {
            log.warn("Could not push {} to {}: {}", payload, destination, e.getMessage());
        }
    }
}
