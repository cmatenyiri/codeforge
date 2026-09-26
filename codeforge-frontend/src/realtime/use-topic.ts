import { type StompSubscription } from '@stomp/stompjs';
import { useCallback, useContext, useEffect, useRef } from 'react';
import { RealtimeContext } from './realtime-context';
import { type ContestChange, type ContestChanged } from './topics';

/**
 * At most one re-read per this window, however many changes arrive in it.
 *
 * <p>A live contest's standings move with every counted verdict from anybody;
 * a screen that re-read on each would turn a busy minute into a request storm.
 */
const COALESCE_MS = 1_000;

/**
 * Subscribes to a topic while the component is mounted and a connection is up.
 *
 * <p>Re-subscribes on every new connection, since a STOMP subscription dies with
 * the connection it was made on. Coming back after a drop also calls
 * `onReconnect`: whatever was pushed while the connection was down is lost, so
 * the screen has to re-read rather than wait to be told.
 *
 * @param destination null to subscribe to nothing, e.g. until an id is known
 */
export const useTopic = <T>(
  destination: string | null,
  onMessage: (message: T) => void,
  onReconnect?: () => void,
): void => {
  const { client, connection } = useContext(RealtimeContext);

  // The latest callbacks, so a re-render does not cost a re-subscription.
  const handlers = useRef({ onMessage, onReconnect });
  useEffect(() => {
    handlers.current = { onMessage, onReconnect };
  });

  const lastSubscribed = useRef<{ destination: string; connection: number } | null>(null);

  useEffect(() => {
    if (client === null || connection === 0 || destination === null || !client.connected) {
      return;
    }

    const subscription: StompSubscription = client.subscribe(destination, (frame) => {
      handlers.current.onMessage(JSON.parse(frame.body) as T);
    });

    const previous = lastSubscribed.current;
    if (previous?.destination === destination && previous.connection !== connection) {
      handlers.current.onReconnect?.();
    }
    lastSubscribed.current = { destination, connection };

    return () => {
      // A subscription unsubscribes on the connection it was made on, which may
      // be the one that just dropped — there is nothing to tell a dead socket.
      try {
        subscription.unsubscribe();
      } catch {
        // Already gone with its connection.
      }
    };
  }, [client, connection, destination]);
};

/**
 * Calls `callback` straight away, or — if it ran less than `intervalMs` ago —
 * once more at the end of that interval, however many calls arrived meanwhile.
 */
export const useThrottled = (callback: () => void, intervalMs: number): (() => void) => {
  const latest = useRef(callback);
  useEffect(() => {
    latest.current = callback;
  });

  const state = useRef<{ last: number; timer: ReturnType<typeof setTimeout> | null }>({ last: 0, timer: null });
  useEffect(
    () => () => {
      if (state.current.timer !== null) {
        clearTimeout(state.current.timer);
      }
    },
    [],
  );

  return useCallback(() => {
    const current = state.current;
    if (current.timer !== null) {
      return;
    }
    const wait = current.last + intervalMs - Date.now();
    if (wait <= 0) {
      current.last = Date.now();
      latest.current();
      return;
    }
    current.timer = setTimeout(() => {
      current.timer = null;
      current.last = Date.now();
      latest.current();
    }, wait);
  }, [intervalMs]);
};

/**
 * Re-reads a contest screen when the contest changes in a way it shows.
 *
 * @param destination `topics.contests`, `topics.contest(id)` or `topics.adminContest(id)`;
 *     null until the id is known
 * @param changes the kinds of change the screen shows; anything else is ignored
 * @param onChange the re-read, throttled; also run after a reconnect
 */
export const useContestChanges = (
  destination: string | null,
  changes: readonly ContestChange[],
  onChange: () => void,
): void => {
  const reread = useThrottled(onChange, COALESCE_MS);

  useTopic<ContestChanged>(
    destination,
    (event) => {
      if (changes.includes(event.change)) {
        reread();
      }
    },
    reread,
  );
};
