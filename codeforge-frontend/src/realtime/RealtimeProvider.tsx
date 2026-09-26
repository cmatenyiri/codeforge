import { Client } from '@stomp/stompjs';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import SockJS from 'sockjs-client';
import { apiBaseUrl } from '../api/client';
import { useAuth } from '../auth/use-auth';
import { RealtimeContext, type RealtimeContextValue } from './realtime-context';

/** How long to wait before reconnecting after the connection drops. */
const RECONNECT_DELAY_MS = 5_000;

/** Both directions; a connection silent for longer than this is treated as dead. */
const HEARTBEAT_MS = 10_000;

/**
 * Holds the one WebSocket connection the app keeps open, for live updates.
 *
 * <p>STOMP over SockJS, to the backend's `/ws`. SockJS is there for the
 * networks that block WebSockets: it falls back to HTTP streaming without the
 * code above it noticing.
 *
 * <p>Open only while somebody is signed in, and opened afresh for each user. The
 * handshake is authenticated by the same httpOnly cookie as every API call, so
 * the connection belongs to whoever was signed in when it opened — signing out,
 * or in as somebody else, has to close it rather than leave it listening on the
 * previous user's behalf.
 *
 * <p>Nothing is ever sent on it. Every message comes from the server, and says
 * only that something changed; the screen then re-reads it over HTTP.
 */
export const RealtimeProvider = ({ children }: { children: ReactNode }) => {
  const { user } = useAuth();
  const userId = user?.id ?? null;

  const [client, setClient] = useState<Client | null>(null);
  const [connection, setConnection] = useState(0);

  useEffect(() => {
    if (userId === null) {
      return;
    }

    const stomp = new Client({
      webSocketFactory: () => new SockJS(`${apiBaseUrl}/ws`),
      reconnectDelay: RECONNECT_DELAY_MS,
      heartbeatIncoming: HEARTBEAT_MS,
      heartbeatOutgoing: HEARTBEAT_MS,
      onConnect: () => {
        setConnection((count) => count + 1);
      },
    });
    stomp.activate();
    setClient(stomp);

    return () => {
      setClient(null);
      void stomp.deactivate();
    };
  }, [userId]);

  const value = useMemo<RealtimeContextValue>(() => ({ client, connection }), [client, connection]);

  return <RealtimeContext.Provider value={value}>{children}</RealtimeContext.Provider>;
};
