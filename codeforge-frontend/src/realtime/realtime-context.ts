import { type Client } from '@stomp/stompjs';
import { createContext } from 'react';

export type RealtimeContextValue = {
  /** The STOMP client, or null while signed out. */
  client: Client | null;
  /**
   * Counts successful connections, starting at 1; 0 until the first.
   *
   * <p>Subscriptions are keyed on it. A STOMP subscription does not survive a
   * dropped connection, so every new connection has to subscribe again — and a
   * screen that was disconnected for a while may have missed what it was told,
   * so it re-reads.
   */
  connection: number;
};

export const RealtimeContext = createContext<RealtimeContextValue>({ client: null, connection: 0 });
