import { useEffect, useRef, useState } from 'react';

/** How often the countdown is re-anchored to the server's answer. */
export const RESYNC_INTERVAL_MS = 20_000;

/**
 * Seconds remaining, for display.
 *
 * <p>The number that decides anything is the server's: every read recomputes it
 * from the stored start time, and every submission re-checks it. This hook
 * exists only so the clock on screen moves between those reads, and it
 * re-anchors on each one — a browser that has been asleep, or whose system clock
 * is wrong, is corrected within one poll.
 *
 * <p>Elapsed wall time is measured rather than ticks counted, because a
 * background tab has its timers throttled to about once a minute and a
 * tick-counting clock would come back minutes slow — which for a contest is the
 * difference between submitting and not.
 *
 * @param seconds the server's latest answer
 * @param running false stops the tick; a finished contest's zero should stay zero
 */
export const useContestClock = (seconds: number | undefined, running: boolean): number => {
  const anchor = useRef({ seconds: seconds ?? 0, at: Date.now() });
  const [remaining, setRemaining] = useState(seconds ?? 0);

  useEffect(() => {
    anchor.current = { seconds: seconds ?? 0, at: Date.now() };
    setRemaining(seconds ?? 0);
  }, [seconds]);

  useEffect(() => {
    if (!running) {
      return;
    }

    const timer = setInterval(() => {
      const elapsed = (Date.now() - anchor.current.at) / 1000;
      setRemaining(Math.max(0, Math.round(anchor.current.seconds - elapsed)));
    }, 1000);

    return () => {
      clearInterval(timer);
    };
  }, [running]);

  return remaining;
};
