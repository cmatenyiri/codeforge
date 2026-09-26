package com.codeforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code codeforge.websocket.*}.
 *
 * @param relay the STOMP broker every instance relays its WebSocket sessions to
 */
@ConfigurationProperties(prefix = "codeforge.websocket")
public record WebSocketProperties(Relay relay) {

    /**
     * @param login used both for the relay's own connection and for the one it
     *     opens per browser session — browsers never present broker credentials
     */
    public record Relay(String host, int port, String login, String passcode, String virtualHost) {}
}
