import { useEffect, useRef, useState } from 'react';
import { type InterviewSession } from '../../api/types';

/** How often the countdown is checked against the server's answer. */
export const RESYNC_INTERVAL_MS = 20_000;

/**
 * Seconds left in the round, for display.
 *
 * <p>The number that decides anything is the server's: every session read
 * recomputes it from the stored start time, and every action re-checks it. This
 * hook exists only so the clock on screen moves between those reads, and it
 * re-anchors to the server on each one — a browser that has been asleep, or
 * whose system clock is wrong, is corrected within one poll.
 *
 * <p>Elapsed wall time is measured rather than ticks counted, because a
 * background tab has its timers throttled to once a minute and a tick-counting
 * clock would come back minutes slow.
 */
export const useInterviewClock = (session: InterviewSession | null): number => {
  const anchor = useRef({ remaining: 0, at: Date.now() });
  const [remaining, setRemaining] = useState(session?.remainingSeconds ?? 0);

  useEffect(() => {
    if (session === null) {
      return;
    }
    anchor.current = { remaining: session.remainingSeconds, at: Date.now() };
    setRemaining(session.remainingSeconds);
  }, [session]);

  useEffect(() => {
    if (session === null || session.status !== 'IN_PROGRESS') {
      return;
    }

    const timer = setInterval(() => {
      const elapsed = (Date.now() - anchor.current.at) / 1000;
      setRemaining(Math.max(0, Math.round(anchor.current.remaining - elapsed)));
    }, 1000);

    return () => {
      clearInterval(timer);
    };
  }, [session]);

  return remaining;
};
